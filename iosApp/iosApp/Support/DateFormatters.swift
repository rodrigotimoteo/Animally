import Foundation
import Shared

/// Single source for all date formatting and LocalDate ↔ Date conversion.
///
/// Consolidates `displayString` / `isoDateFormatter` / `friendlyString` / `shortFmt`
/// and the `swiftDate` noon trick + `TodayProvider` duplication into one primitive.
enum DateFormatters {
    static let iso: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        f.timeZone = TimeZone(secondsFromGMT: 0)
        f.calendar = Calendar(identifier: .gregorian)
        return f
    }()

    static let friendly: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "d MMM yyyy"
        f.timeZone = TimeZone(secondsFromGMT: 0)
        f.calendar = Calendar(identifier: .gregorian)
        return f
    }()

    static let short: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "dd MMM"
        f.timeZone = TimeZone(secondsFromGMT: 0)
        f.calendar = Calendar(identifier: .gregorian)
        return f
    }()

    /// Noon avoids DST edge shifts (spring-forward midnight doesn't exist).
    static let middayHour = 12

    private static var gregorianUTC: Calendar {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone(secondsFromGMT: 0)!
        return c
    }

    static func date(from localDate: Kotlinx_datetimeLocalDate) -> Date {
        var comps = DateComponents()
        comps.year = Int(localDate.year)
        comps.month = Int(localDate.month.ordinal) + 1
        comps.day = Int(localDate.day)
        comps.hour = middayHour
        return gregorianUTC.date(from: comps) ?? Date()
    }

    static func localDate(from date: Date) -> Kotlinx_datetimeLocalDate {
        let comps = gregorianUTC.dateComponents([.year, .month, .day], from: date)
        return Kotlinx_datetimeLocalDate(
            year: Int32(comps.year ?? 1970),
            month: Int32(comps.month ?? 1),
            day: Int32(comps.day ?? 1)
        )
    }

    static func todayLocalDate() -> Kotlinx_datetimeLocalDate {
        localDate(from: Date())
    }

    static func todayAtNoon() -> Date {
        date(from: todayLocalDate())
    }
}

// MARK: - Kotlinx_datetimeLocalDate helpers — single source

extension Kotlinx_datetimeLocalDate {
    /// ISO yyyy-MM-dd — single formatter, not String(format:) duplication.
    var displayString: String {
        DateFormatters.iso.string(from: DateFormatters.date(from: self))
    }

    /// Friendly "5 Jan 2024" — same formatter primitive.
    var friendlyString: String {
        DateFormatters.friendly.string(from: DateFormatters.date(from: self))
    }

    /// Short "05 Jan" for charts.
    var shortString: String {
        DateFormatters.short.string(from: DateFormatters.date(from: self))
    }

    /// Noon Date to avoid DST gaps — single primitive.
    var swiftDate: Date {
        DateFormatters.date(from: self)
    }

    /// Epoch days via proleptic Gregorian — stable for hashing.
    func epochDaysCompat() -> Int64 {
        let y = Int(year)
        let m = Int(month.ordinal) + 1
        let d = Int(day)
        let yy = m <= 2 ? y - 1 : y
        let mm = m <= 2 ? m + 12 : m
        let era = (yy >= 0 ? yy : yy - 399) / 400
        let yoe = yy - era * 400
        let doy = (153 * (mm - 3) + 2) / 5 + d - 1
        let doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return Int64(era * 146097 + doe - 719468)
    }

    func daysUntil(_ other: Kotlinx_datetimeLocalDate) -> Int {
        Int(other.epochDaysCompat() - self.epochDaysCompat())
    }
}

extension Date {
    var kotlinLocalDate: Kotlinx_datetimeLocalDate {
        DateFormatters.localDate(from: self)
    }
}
