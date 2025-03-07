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
  print("Found \(remoteImageURLs.count) images to download")

  let workspace = NSWorkspace()
  let localImageURLs =
    try await remoteImageURLs
    .concurrentCompactMap({ url in try await URLSession.shared.download(from: url).0 })
    .reversed()

  print("Downloaded \(localImageURLs.count) images")

  for (image, screen) in zip(localImageURLs, screens) {
    try workspace.setDesktopImageURL(
      image,
      for: screen,
      options: [
        .allowClipping: NSNumber(true),
        .imageScaling: NSNumber(value: NSImageScaling.scaleProportionallyUpOrDown.rawValue),
      ])
  }
  print("Set \(screens.count) desktop images")
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

  let (apodData, response) = try await URLSession.shared.data(from: apodURL)
  if let httpResponse = response as? HTTPURLResponse {
    if httpResponse.statusCode >= 300 {
      throw ApodError.apiGetFailed(
        message:
          "bad status code while fetching list of imagess from NASA: \(httpResponse.statusCode)"
      )
    }
  } else {
    throw ApodError.apiGetFailed(message: "bad response from NASA while fetching list of images")
  }

  let decoder = JSONDecoder()
  let apodItems = try decoder.decode([ApodEntry].self, from: apodData)
    .filter({ $0.media_type == .image })
    .map({ apod in apod.hdurl ?? apod.url })

  return apodItems
}

enum ApodError: Error {
  case badApiURL
  case badImageURL
  case apiGetFailed(message: String)
}

extension ApodError: CustomStringConvertible {
  var description: String {
    switch self {
    case .badApiURL: return "API URL is bad"
    case .badImageURL: return "Image URL is bad"
    case .apiGetFailed(let message): return message
    }
  }
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
