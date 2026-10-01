//
//  main.swift
//  APODesktop
//
//  Created by Max Coplan on 8/31/22.
//

import AppKit
import Foundation

@main
@MainActor
struct APODesktop {
  static func main() async throws {
    let screens = NSScreen.screens
    if screens.isEmpty {
      throw ApodError(description: "No screens found")
    }

    // Request two spare entries because some APOD entries are videos.
    let entryCount = screens.count + 2
    print("Fetching \(entryCount) recent APOD entries")
    let remoteImageURLs = try await getApodImageURLs(count: entryCount)
    print("Found \(remoteImageURLs.count) images to download")
    if remoteImageURLs.count == 0 {
      throw ApodError(description: "No images found")
    }

    let workspace = NSWorkspace()
    let localImageURLs =
      await remoteImageURLs
      .concurrentCompactMap({ url in await downloadImage(from: url) })

    print("Downloaded \(localImageURLs.count) images")
    if localImageURLs.count == 0 {
      throw ApodError(description: "No valid images found")
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
  }
}

func downloadImage(from url: URL) async -> URL? {
  do {
    let (localURL, response) = try await URLSession.shared.download(from: url)
    guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
      print("Failed to download from \(url): HTTP \((response as? HTTPURLResponse)?.statusCode ?? -1)")
      try? FileManager.default.removeItem(at: localURL)
      return nil
    }
    guard response.mimeType?.hasPrefix("image/") == true else {
      print("Downloaded file from \(url) is not an image (MIME type: \(response.mimeType ?? "unknown"))")
      try? FileManager.default.removeItem(at: localURL)
      return nil
    }
    return localURL
  } catch {
    print("Failed to download from \(url): \(error)")
    return nil
  }
}

func getApodImageURLs(count: Int) async throws -> [URL] {
  let apodUrlPath = "https://science.nasa.gov/wp-json/wp/v2/apod-basic"
  // The fixed HTTPS URL and integer count always form a valid URL.
  let apodURL = URL(string: "\(apodUrlPath)?per_page=\(count)")!

  let (apodData, response) = try await URLSession.shared.data(from: apodURL)
  guard let http = response as? HTTPURLResponse else {
    throw ApodError(description: "Non-HTTP response while fetching APOD entries")
  }
  guard http.statusCode == 200 else {
    throw ApodError(description: "HTTP \(http.statusCode) while fetching APOD entries")
  }

  let decoder = JSONDecoder()
  let apodItems = try decoder.decode([ApodEntry].self, from: apodData)
    .sorted(by: { $0.date > $1.date })
    .filter({ $0.media_type == "image" })
    .compactMap({ apod in apod.hdurl })

  return apodItems
}

struct ApodError: Error, CustomStringConvertible {
  let description: String
}

struct ApodEntry: Decodable {
  let date: String
  let hdurl: URL?
  let media_type: String?
}
