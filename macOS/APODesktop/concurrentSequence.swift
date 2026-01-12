//
//  util.swift
//  APODesktop
//
//  Created by Max Coplan on 8/31/22.
//

extension Sequence {
  func concurrentCompactMap<T>(
    _ transform: @escaping (Element) async throws -> T?
  ) async rethrows -> [T] {
    try await withThrowingTaskGroup(of: (Int, T?).self) { group in
      for (index, element) in self.enumerated() {
        group.addTask {
          (index, try await transform(element))
        }
      }

      var indexedResults: [(Int, T?)] = []
      for try await result in group {
        indexedResults.append(result)
      }

      return indexedResults
        .sorted(by: { lhs, rhs in lhs.0 < rhs.0 })
        .compactMap { element in element.1 }
    }
  }
}
