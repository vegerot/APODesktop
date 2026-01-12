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
    // Check if we should run as a daemon (monitor for screen changes)
    if CommandLine.arguments.contains("--daemon") {
      let daemon = ScreenChangeMonitor()
      daemon.startMonitoring()
      RunLoop.main.run()
    } else {
      // One-time execution
      let _ = try await Main()
    }
  }
}

class ScreenChangeMonitor {
  private var lastScreenCount: Int = 0
  
  func startMonitoring() {
    lastScreenCount = NSScreen.screens.count
    print("Starting monitor detection daemon with \(lastScreenCount) screens")
    
    // Update wallpaper immediately on start
    Task {
      let _ = try? await Main()
    }
    
    // Listen for screen configuration changes
    NotificationCenter.default.addObserver(
      forName: NSApplication.didChangeScreenParametersNotification,
      object: nil,
      queue: .main
    ) { [weak self] _ in
      self?.handleScreenChange()
    }
  }
  
  private func handleScreenChange() {
    let currentScreenCount = NSScreen.screens.count
    if currentScreenCount != lastScreenCount {
      print("Screen count changed from \(lastScreenCount) to \(currentScreenCount)")
      lastScreenCount = currentScreenCount
      
      // Update wallpaper when screen count changes
      Task {
        let _ = try? await Main()
      }
    }
  }
}

func Main() async throws -> Result<Bool, ApodError> {

  let screens = NSScreen.screens
  if screens.isEmpty {
    throw ApodError.expectationFailed(message: "No screens found")
  }

  /// shit happens (sometimes it's a video)
  let daysToLookBack = screens.count + 2
  let dateNDaysAgo: Date = .init(timeIntervalSinceNow: .init(-1 * daysToLookBack * 60 * 60 * 24))
  print("Looking back \(daysToLookBack) days from \(dateNDaysAgo)")
  let remoteImageURLs = try await getApodImageURLs(from: dateNDaysAgo)
  print("Found \(remoteImageURLs.count) images to download")
  if remoteImageURLs.count == 0 {
    throw ApodError.expectationFailed(message: "No images found")
  }

  let workspace = NSWorkspace()
  let localImageURLs =
    try await remoteImageURLs
    .concurrentCompactMap({ url in try await downloadImage(from: url) })
    .reversed()

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
  print("Set \(screens.count) desktop images")
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

func getApodImageURLs(from date: Date) async throws -> [URL] {
  let dateFormatter: DateFormatter = .init()
  dateFormatter.locale = Locale(identifier: "en_US_POSIX")
  dateFormatter.dateFormat = "yyyy-MM-dd"

  let apodDate = dateFormatter.string(from: date)

  let apodUrlPath = "https://api.nasa.gov/planetary/apod"
  // TODO: remove secret
  let apiKey = "JvhDwQU1Uhv7yfaQTSqcsncZjwF5ZJR6McrzVE4f"

  guard
    let apodURL = URL(
      string:
        "\(apodUrlPath)?api_key=\(apiKey)&start_date=\(apodDate)"
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
    .compactMap({ apod in apod.hdurl ?? apod.url })

  return apodItems
}

enum ApodError: Error {
  case badApiURL
  case badImageURL
  case apiGetFailed(message: String)
  case expectationFailed(message: String)
}

extension ApodError: CustomStringConvertible {
  var description: String {
    switch self {
    case .badApiURL: return "API URL is bad"
    case .badImageURL: return "Image URL is bad"
    case .apiGetFailed(let message): return message
    case .expectationFailed(let message): return message
    }
  }
}

struct ApodEntry: Codable {
  let hdurl: URL?
  let url: URL?
  let media_type: ApodMediaType?

  enum ApodMediaType: String, Codable {
    case image
    case video
    case other
    // it would be nice to have an `unknown` default option.
    // Now the program will crash if an unexpectd media_type is returned
  }
}
