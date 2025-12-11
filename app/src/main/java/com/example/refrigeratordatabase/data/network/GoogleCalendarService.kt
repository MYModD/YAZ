package com.example.refrigeratordatabase.data.network

import android.util.Log
import com.example.refrigeratordatabase.data.auth.GoogleAuthService
import com.example.refrigeratordatabase.data.model.CalendarEvent
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * GoogleCalendarService - Google Calendar API クライアント
 *
 * PHPでいう「CURLでGoogle Calendar APIを叩く処理」に相当。
 * ```php
 * $ch = curl_init();
 * curl_setopt($ch, CURLOPT_URL, "https://www.googleapis.com/calendar/v3/calendars/primary/events");
 * curl_setopt($ch, CURLOPT_HTTPHEADER, array("Authorization: Bearer " . $accessToken));
 * $response = curl_exec($ch);
 * $events = json_decode($response, true);
 * ```
 *
 * これをAndroidではGoogle Calendar APIクライアントライブラリで実現する。
 */
class GoogleCalendarService(
    private val authService: GoogleAuthService,
    private val context: android.content.Context
) {
    companion object {
        private const val TAG = "GoogleCalendarService"
        private const val APPLICATION_NAME = "RefrigeratorDatabase"
        
        // 専用カレンダーの設定
        private const val FOOD_CALENDAR_NAME = "食材"
        private const val FOOD_CALENDAR_COLOR_ID = "5"  // バナナ色（黄色）
    }

    private val httpTransport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()
    
    // 専用カレンダーIDをキャッシュ（毎回検索しないように）
    private var foodCalendarId: String? = null

    /**
     * 指定した年月のカレンダーイベントを取得
     * PHPでいう: CURLでAPIを叩いてJSONをパース
     *
     * @param year 年
     * @param month 月（1-12）
     * @return イベントのリスト
     */
    suspend fun getEvents(year: Int, month: Int): Result<List<CalendarEvent>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching events for $year/$month")

            val account = authService.getCurrentAccount()
            if (account == null) {
                Log.w(TAG, "No signed-in account")
                return@withContext Result.failure(Exception("Not signed in"))
            }

            // GoogleAccountCredentialを作成（読み書き両用スコープ）
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(CalendarScopes.CALENDAR)  // 読み書き両用に変更
            )
            credential.selectedAccount = account.account

            // Calendar APIクライアントを構築
            // PHPでいう: new Google_Service_Calendar($client)
            val calendarService = Calendar.Builder(
                httpTransport,
                jsonFactory,
                credential
            )
                .setApplicationName(APPLICATION_NAME)
                .build()

            // 月の開始日と終了日を計算
            val startOfMonth = java.util.Calendar.getInstance().apply {
                set(year, month - 1, 1, 0, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val endOfMonth = java.util.Calendar.getInstance().apply {
                set(year, month - 1, 1, 0, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                add(java.util.Calendar.MONTH, 1)
            }

            val timeMin = DateTime(startOfMonth.timeInMillis)
            val timeMax = DateTime(endOfMonth.timeInMillis)

            Log.d(TAG, "Time range: $timeMin to $timeMax")

            // イベントを取得
            // PHPでいう: $service->events->listEvents('primary', ['timeMin' => ...])
            val events = calendarService.events()
                .list("primary") // プライマリカレンダー
                .setTimeMin(timeMin)
                .setTimeMax(timeMax)
                .setSingleEvents(true) // 繰り返しイベントを個別に展開
                .setOrderBy("startTime")
                .setMaxResults(100)
                .execute()

            val calendarEvents = events.items?.mapNotNull { event ->
                try {
                    val startDateTime = event.start?.dateTime ?: event.start?.date
                    val endDateTime = event.end?.dateTime ?: event.end?.date
                    
                    if (startDateTime == null || endDateTime == null) {
                        Log.w(TAG, "Event ${event.id} has no start/end time")
                        return@mapNotNull null
                    }

                    val isAllDay = event.start?.date != null

                    CalendarEvent(
                        id = event.id ?: "",
                        title = event.summary ?: "(タイトルなし)",
                        startTime = startDateTime.value,
                        endTime = endDateTime.value,
                        isAllDay = isAllDay
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse event: ${event.id}", e)
                    null
                }
            } ?: emptyList()

            Log.d(TAG, "Fetched ${calendarEvents.size} events")
            Result.success(calendarEvents)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch events", e)
            Result.failure(e)
        }
    }

    /**
     * 指定した日のイベントを取得
     *
     * @param timestamp 日付のタイムスタンプ
     * @return その日のイベントのリスト
     */
    suspend fun getEventsForDay(timestamp: Long): Result<List<CalendarEvent>> = withContext(Dispatchers.IO) {
        try {
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = timestamp
            }
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH) + 1

            val result = getEvents(year, month)
            
            result.map { events ->
                events.filter { event ->
                    val eventCal = java.util.Calendar.getInstance().apply {
                        timeInMillis = event.startTime
                    }
                    val targetCal = java.util.Calendar.getInstance().apply {
                        timeInMillis = timestamp
                    }
                    
                    eventCal.get(java.util.Calendar.YEAR) == targetCal.get(java.util.Calendar.YEAR) &&
                    eventCal.get(java.util.Calendar.MONTH) == targetCal.get(java.util.Calendar.MONTH) &&
                    eventCal.get(java.util.Calendar.DAY_OF_MONTH) == targetCal.get(java.util.Calendar.DAY_OF_MONTH)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch events for day", e)
            Result.failure(e)
        }
    }

    /**
     * イベントがある日付のセットを取得（ドットマーカー表示用）
     *
     * @param year 年
     * @param month 月（1-12）
     * @return イベントがある日のタイムスタンプのセット
     */
    suspend fun getEventDates(year: Int, month: Int): Result<Set<Long>> = withContext(Dispatchers.IO) {
        getEvents(year, month).map { events ->
            events.map { event ->
                // 日付のみを取得（時間を0に正規化）
                java.util.Calendar.getInstance().apply {
                    timeInMillis = event.startTime
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
            }.toSet()
        }
    }

    /**
     * 「食材」専用カレンダーを取得または作成
     * PHPでいう: SELECT or INSERT パターン
     * 
     * ```php
     * $calendar = findCalendarByName('食材');
     * if (!$calendar) {
     *     $calendar = createCalendar('食材', 'banana');
     * }
     * ```
     *
     * @param calendarService Calendar APIクライアント
     * @return 専用カレンダーのID
     */
    private fun getOrCreateFoodCalendar(calendarService: Calendar): String {
        // キャッシュがあればそれを使用
        foodCalendarId?.let { return it }

        try {
            // 既存のカレンダーリストから「食材」カレンダーを検索
            val calendarList = calendarService.calendarList().list().execute()
            val existingCalendar = calendarList.items?.find { it.summary == FOOD_CALENDAR_NAME }

            if (existingCalendar != null) {
                Log.d(TAG, "Found existing food calendar: ${existingCalendar.id}")
                foodCalendarId = existingCalendar.id
                return existingCalendar.id
            }

            // 存在しない場合は新規作成
            Log.d(TAG, "Creating new food calendar...")
            val newCalendar = com.google.api.services.calendar.model.Calendar().apply {
                summary = FOOD_CALENDAR_NAME
                description = "冷蔵庫データベースアプリの食材期限管理用カレンダー"
                timeZone = "Asia/Tokyo"
            }

            val createdCalendar = calendarService.calendars().insert(newCalendar).execute()
            Log.d(TAG, "Created new food calendar: ${createdCalendar.id}")

            // カレンダーリストエントリーを更新してバナナ色を設定
            val calendarListEntry = calendarService.calendarList().get(createdCalendar.id).execute()
            calendarListEntry.colorId = FOOD_CALENDAR_COLOR_ID
            calendarService.calendarList().update(createdCalendar.id, calendarListEntry).execute()
            Log.d(TAG, "Set calendar color to Banana (colorId: $FOOD_CALENDAR_COLOR_ID)")

            foodCalendarId = createdCalendar.id
            return createdCalendar.id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get or create food calendar, falling back to primary", e)
            // エラー時はプライマリカレンダーにフォールバック
            return "primary"
        }
    }

    /**
     * 食材の期限イベントをGoogleカレンダーに追加
     * PHPでいう: curl -X POST でCalendar APIにイベントを登録
     *
     * ```php
     * $event = [
     *     'summary' => '食材名 期限切れ',
     *     'start' => ['date' => '2024-12-25'],
     *     'end' => ['date' => '2024-12-25']
     * ];
     * curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($event));
     * ```
     *
     * @param foodName 食材名
     * @param expiryDate 期限日のタイムスタンプ
     * @return 作成されたイベントのID（成功時）、エラー（失敗時）
     */
    suspend fun addFoodExpiryEvent(foodName: String, expiryDate: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Adding expiry event for: $foodName")

            val account = authService.getCurrentAccount()
            if (account == null) {
                Log.w(TAG, "No signed-in account")
                return@withContext Result.failure(Exception("Not signed in"))
            }

            // GoogleAccountCredentialを作成（読み書き両用スコープ）
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(CalendarScopes.CALENDAR)
            )
            credential.selectedAccount = account.account

            // Calendar APIクライアントを構築
            val calendarService = Calendar.Builder(
                httpTransport,
                jsonFactory,
                credential
            )
                .setApplicationName(APPLICATION_NAME)
                .build()

            // 期限日を日付文字列に変換（終日イベント用）
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = expiryDate
            }
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH) + 1
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            val dateString = String.format("%04d-%02d-%02d", year, month, day)

            // 翌日（終日イベントの終了日として必要）
            val nextDay = java.util.Calendar.getInstance().apply {
                timeInMillis = expiryDate
                add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            val endYear = nextDay.get(java.util.Calendar.YEAR)
            val endMonth = nextDay.get(java.util.Calendar.MONTH) + 1
            val endDay = nextDay.get(java.util.Calendar.DAY_OF_MONTH)
            val endDateString = String.format("%04d-%02d-%02d", endYear, endMonth, endDay)

            // 「食材」専用カレンダーを取得または作成
            val targetCalendarId = getOrCreateFoodCalendar(calendarService)

            // イベントを作成
            val event = Event().apply {
                summary = "🍴 ${foodName} 期限切れ"
                description = "冷蔵庫データベースアプリから追加された食材の期限日です。"
                
                // 終日イベントとして設定
                start = EventDateTime().apply {
                    date = DateTime(dateString)
                }
                end = EventDateTime().apply {
                    date = DateTime(endDateString)
                }
            }

            // イベントを「食材」カレンダーに追加
            val createdEvent = calendarService.events()
                .insert(targetCalendarId, event)
                .execute()

            Log.d(TAG, "Event created: ${createdEvent.id}")
            Result.success(createdEvent.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add event", e)
            Result.failure(e)
        }
    }

    /**
     * Googleカレンダーからイベントを削除
     *
     * @param eventId 削除するイベントのID
     * @return 成功/失敗
     */
    suspend fun deleteEvent(eventId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deleting event: $eventId")

            val account = authService.getCurrentAccount()
            if (account == null) {
                Log.w(TAG, "No signed-in account")
                return@withContext Result.failure(Exception("Not signed in"))
            }

            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(CalendarScopes.CALENDAR)
            )
            credential.selectedAccount = account.account

            val calendarService = Calendar.Builder(
                httpTransport,
                jsonFactory,
                credential
            )
                .setApplicationName(APPLICATION_NAME)
                .build()

            // 「食材」専用カレンダーを取得
            val targetCalendarId = getOrCreateFoodCalendar(calendarService)

            calendarService.events()
                .delete(targetCalendarId, eventId)
                .execute()

            Log.d(TAG, "Event deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete event", e)
            Result.failure(e)
        }
    }
}

