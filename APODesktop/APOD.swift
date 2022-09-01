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

func getApodURLs() async throws -> Result<[URL], ApodError> {
  guard
    let apodURL = URL(
      string:
        "https://api.nasa.gov/planetary/apod?api_key=JvhDwQU1Uhv7yfaQTSqcsncZjwF5ZJR6McrzVE4f&start_date=2022-08-28"
    )
  else {
    return .failure(.badApiURL)
  }
  let res: Result<(Data, URLResponse), ApodError> = await Result.init(catchingAsync: {
    try await URLSession.shared.data(from: apodURL)
  })
  .mapError({ (err: Error) in ApodError.apiGetFailed })

  let decoder = JSONDecoder()
  let apodItems = Result { try decoder.decode([ApodEntry].self, from: res.get().0) }.map({
    $0
      .filter({ $0.media_type == .image }).map({ apod in apod.hdurl ?? apod.url })
  })

  return apodItems
}

func stuff() async throws -> Result<Bool, ApodError> {

  throw "hi"

  let screens = NSScreen.screens
  let firstScreen = screens[0]

  let workspace = NSWorkspace()
  let (localImageURL, _) = try await URLSession.shared.download(from: apodItems[0])
  try workspace.setDesktopImageURL(localImageURL, for: firstScreen)
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

extension Result {
  init(catchingAsync body: () async throws -> Success) async {
    do {
      self = .success(try await body())
    } catch {
      // TODO: remove force unwrap
      self = .failure(error as! Failure)
    }
  }
}
