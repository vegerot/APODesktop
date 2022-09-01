import AppKit
import Foundation

func main() {
  let screens = NSScreen.screens
  let firstScreen = screens[0]

  let workspace = NSWorkspace()
  guard
    let imageURL = URL(
      string: "https://apod.nasa.gov/apod/image/2208/SiccarPoint_CuriosityGill_7640.jpg")
  else {
    return
  }
  try? workspace.setDesktopImageURL(imageURL, for: firstScreen)

}

main()
