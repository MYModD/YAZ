package com.example.refrigeratordatabase.data.model

/**
 * CalendarEvent - Googleカレンダーのイベントを表すデータクラス
 *
 * PHPでいう「連想配列」または「オブジェクト」に相当。
 * Google Calendar APIから取得したイベントをアプリ内で扱いやすい形式に変換したもの。
 *
 * @param id イベントのユニークID（Google Calendar APIから取得）
 * @param title イベントのタイトル（summary）
 * @param startTime イベント開始時刻（Unixタイムスタンプ）
 * @param endTime イベント終了時刻（Unixタイムスタンプ）
 * @param isAllDay 終日イベントかどうか
 */
data class CalendarEvent(
    val id: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val isAllDay: Boolean = false
)

