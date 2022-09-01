//
//  main.swift
//  APODesktop
//
//  Created by Max Coplan on 8/31/22.
//

import AppKit
import Foundation

@main
struct APODesktop {
  static func main() async throws {
    let _ = try await Main()
  }
}

func Main() async throws -> Result<Bool, ApodError> {

  let screens = NSScreen.screens

  /// shit happens (sometimes it's a video)
  let daysToLookBack = screens.count + 2
  let dateNDaysAgo: Date = .init(timeIntervalSinceNow: .init(-1 * daysToLookBack * 60 * 60 * 24))
  let remoteImageURLs = try await getApodImageURLs(from: dateNDaysAgo)

  let workspace = NSWorkspace()
  let localImageURLs =
    try await remoteImageURLs
    .concurrentCompactMap({ url in try await URLSession.shared.download(from: url).0 })
    .reversed()

  for (image, screen) in zip(localImageURLs, screens) {
    try workspace.setDesktopImageURL(
      image,
      for: screen,
      options: [
        .allowClipping: NSNumber(true),
        .imageScaling: NSNumber(value: NSImageScaling.scaleProportionallyUpOrDown.rawValue),
      ])
  }

  return .success(true)
}

func getApodImageURLs(from date: Date) async throws -> [URL] {
  let dateFormatter: DateFormatter = .init()
  dateFormatter.locale = Locale(identifier: "en_US_POSIX")
  dateFormatter.dateFormat = "yyyy-MM-dd"

  let apodDate = dateFormatter.string(from: date)

  guard
    let apodURL = URL(
      string:
        // TODO: remove secret
        "https://api.nasa.gov/planetary/apod?api_key=JvhDwQU1Uhv7yfaQTSqcsncZjwF5ZJR6McrzVE4f&start_date=\(apodDate)"
    )
  else {
    throw ApodError.badApiURL
  }

  let (apodData, _) = try await URLSession.shared.data(from: apodURL)

  let decoder = JSONDecoder()
  let apodItems = try decoder.decode([ApodEntry].self, from: apodData)
    .filter({ $0.media_type == .image })
    .map({ apod in apod.hdurl ?? apod.url })

  return apodItems
}

enum ApodError: Error {
  case badApiURL
  case badImageURL
  case apiGetFailed
}

struct ApodEntry: Codable {
  let hdurl: URL?
  let url: URL
  let media_type: ApodMediaType?

  enum ApodMediaType: String, Codable {
    case image
    case video
    // it would be nice to have an `unknown` default option.
    // Now the program will crash if an unexpectd media_type is returned
  }
}
