// Runs transforms concurrently and collects successful values in input order.
extension Sequence where Element: Sendable {
  func concurrentCompactMap<T: Sendable>(
    _ transform: @escaping @Sendable (Element) async -> T?
  ) async -> [T] {
    let tasks = map { element in Task { await transform(element) } }
    var results: [T] = []
    for task in tasks {
      if let value = await task.value {
        results.append(value)
      }
    }
    return results
  }
}
