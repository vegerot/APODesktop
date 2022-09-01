//
//  util.swift
//  APODesktop
//
//  Created by Max Coplan on 8/31/22.
//

extension Sequence {
  func concurrentCompactMap<T>(
    _ transform: @escaping (Element) async throws -> T
  ) async rethrows -> [T] {
    let tasks = self.map({ e in
      Task {
        try await transform(e)
      }
    })

    var results = [Result<T, Error>]()

    for task in tasks {
      results.append(await task.result)
    }

    return results.compactMap({ result in try? result.get() })
  }
}
