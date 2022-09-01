import AppKit
//
//  main.swift
//  APODesktop
//
//  Created by Max Coplan on 8/31/22.
//
import Foundation

@main
struct APODesktop {
  static func main() async throws {
    try await stuff()
  }
}

enum ApodError: Error {
  case badApiURL
  case badImageURL
  case apiGetFailed

}

func getApodImageURLs() async throws -> [URL] {
  guard
    let apodURL = URL(
      string:
        "https://api.nasa.gov/planetary/apod?api_key=JvhDwQU1Uhv7yfaQTSqcsncZjwF5ZJR6McrzVE4f&start_date=2022-08-27"
    )
  else {
    throw ApodError.badApiURL
  }

  let (apodData, _) = try await URLSession.shared.data(from: apodURL)

  let decoder = JSONDecoder()
  let apodItems = try decoder.decode([ApodEntry].self, from: apodData)
    .filter({ $0.media_type == .image }).map({ apod in apod.hdurl ?? apod.url })

  return apodItems
}

func stuff() async throws -> Result<Bool, ApodError> {

  let apodItems = try await getApodImageURLs()
  let screens = NSScreen.screens

  let workspace = NSWorkspace()
  let localImageURLs =
    try await apodItems
    .concurrentCompactMap({ url in try await URLSession.shared.download(from: url).0 })

  for (image, screen) in zip(localImageURLs, screens) {
    try! workspace.setDesktopImageURL(
      image, for: screen,
      options: [
        .allowClipping: NSNumber(true),
        .imageScaling: NSNumber(value: NSImageScaling.scaleProportionallyUpOrDown.rawValue),
      ])
  }

  return .success(true)
}

struct ApodEntry: Codable {
  let hdurl: URL?
  let url: URL
  let media_type: ApodMediaType?

  enum ApodMediaType: String, Codable {
    case image
    case video
    // it would be nice to have an `unknown` default option

  }
}
