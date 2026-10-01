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
  if screens.isEmpty {
    throw ApodError.expectationFailed(message: "No screens found")
  }

  // Request two spare entries because some APOD entries are videos.
  let entryCount = screens.count + 2
  print("Fetching \(entryCount) recent APOD entries")
  let remoteImageURLs = try await getApodImageURLs(count: entryCount)
  print("Found \(remoteImageURLs.count) images to download")
  if remoteImageURLs.count == 0 {
    throw ApodError.expectationFailed(message: "No images found")
  }

  let workspace = NSWorkspace()
  let localImageURLs =
    try await remoteImageURLs
    .concurrentCompactMap({ url in try await downloadImage(from: url) })

  print("Downloaded \(localImageURLs.count) images")
  if localImageURLs.count == 0 {
    throw ApodError.expectationFailed(message: "No valid images found")
  }

  for (image, screen) in zip(localImageURLs, screens) {
    try workspace.setDesktopImageURL(
      image,
      for: screen,
      options: [
        .allowClipping: NSNumber(true),
        .imageScaling: NSNumber(value: NSImageScaling.scaleProportionallyUpOrDown.rawValue),
      ])
  }
  print("Set \(min(localImageURLs.count, screens.count)) desktop images")
  return .success(true)
}

func downloadImage(from url: URL) async throws -> URL? {
  let (tempLocalURL, response) = try await URLSession.shared.download(from: url)

  guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
    print("Failed to download from \(url): \((response as? HTTPURLResponse)?.statusCode ?? -1)")
    return nil
  }

  // Check MIME type to ensure it's an image
  if let mimeType = httpResponse.mimeType, mimeType.hasPrefix("image") {
    return tempLocalURL
  } else {
    print(
      "Downloaded file from \(url) is not an image (MIME type: \(httpResponse.mimeType ?? "unknown"))"
    )
    // Try to remove the downloaded file if it's not an image and we don't want it
    try? FileManager.default.removeItem(at: tempLocalURL)
    return nil
  }
}

func getApodImageURLs(count: Int) async throws -> [URL] {
  let apodUrlPath = "https://science.nasa.gov/wp-json/wp/v2/apod-basic"
  // The fixed HTTPS URL and integer count always form a valid URL.
  let apodURL = URL(string: "\(apodUrlPath)?per_page=\(count)")!

  let (apodData, response) = try await URLSession.shared.data(from: apodURL)
  if let httpResponse = response as? HTTPURLResponse {
    if httpResponse.statusCode >= 300 {
      throw ApodError.apiGetFailed(
        message:
          "bad status code while fetching list of images from NASA: \(httpResponse.statusCode)"
      )
    }
  } else {
    throw ApodError.apiGetFailed(message: "bad response from NASA while fetching list of images")
  }

  let decoder = JSONDecoder()
  let apodItems = try decoder.decode([ApodEntry].self, from: apodData)
    .sorted(by: { $0.date > $1.date })
    .filter({ $0.media_type == "image" })
    .compactMap({ apod in apod.hdurl })

  return apodItems
}

enum ApodError: Error {
  case badImageURL
  case apiGetFailed(message: String)
  case expectationFailed(message: String)
}

extension ApodError: CustomStringConvertible {
  var description: String {
    switch self {
    case .badImageURL: return "Image URL is bad"
    case .apiGetFailed(let message): return message
    case .expectationFailed(let message): return message
    }
  }
}

struct ApodEntry: Decodable {
  let date: String
  let hdurl: URL?
  let media_type: String?
}
