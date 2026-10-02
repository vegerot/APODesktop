using System.Net.Http.Json;
using System.Runtime.InteropServices;
using System.Text.Json.Serialization;

IDesktopWallpaper desktop = (IDesktopWallpaper)(object)new DesktopWallpaperClass();
desktop.GetMonitorDevicePathCount(out uint monitorCount);
if (monitorCount == 0)
{
    throw new InvalidOperationException("No monitors found.");
}

List<string> monitors = [];
for (uint i = 0; i < monitorCount; ++i)
{
    desktop.GetMonitorDevicePathAt(i, out string monitor);
    monitors.Add(monitor);
}
Console.WriteLine($"Detected {monitors.Count} monitors");

// Request two spare entries because some APOD entries are videos.
int entryCount = monitors.Count + 2;
Console.WriteLine($"Fetching {entryCount} recent APOD entries");
using HttpClient httpClient = new();
List<ApodEntry> entries = await httpClient.GetFromJsonAsync(
    $"https://science.nasa.gov/wp-json/wp/v2/apod-basic?per_page={entryCount}",
    ApodApiJsonContext.Default.ListApodEntry)
    ?? throw new InvalidOperationException("APOD API returned no data.");

List<string> urls = entries
    .OrderByDescending(entry => entry.Date, StringComparer.Ordinal)
    .Where(entry => entry is { MediaType: "image", HdUrl: not null })
    .Select(entry => entry.HdUrl!)
    .ToList();
Console.WriteLine($"Found {urls.Count} images to download");
if (urls.Count == 0)
{
    throw new InvalidOperationException("No images found.");
}

string tempDirectory = Path.Combine(Path.GetTempPath(), Path.GetRandomFileName());
Directory.CreateDirectory(tempDirectory);
List<string> images = [];
foreach (string url in urls)
{
    try
    {
        Console.WriteLine($"Downloading {url}...");
        using HttpResponseMessage response = await httpClient.GetAsync(url);
        response.EnsureSuccessStatusCode();
        string? mediaType = response.Content.Headers.ContentType?.MediaType;
        if (mediaType?.StartsWith("image/", StringComparison.OrdinalIgnoreCase) != true)
        {
            Console.WriteLine($"Skipping non-image from {url} (MIME type: {mediaType ?? "unknown"})");
            continue;
        }

        string imagePath = Path.Combine(tempDirectory, $"{images.Count}.jpeg");
        byte[] imageBytes = await response.Content.ReadAsByteArrayAsync();
        Console.WriteLine($"Downloaded {imageBytes.Length} bytes ({mediaType})");
        await File.WriteAllBytesAsync(imagePath, imageBytes);
        images.Add(imagePath);
        if (images.Count == monitors.Count)
        {
            break;
        }
    }
    catch (HttpRequestException error)
    {
        Console.WriteLine($"Failed to download from {url}: {error.Message}");
    }
    catch (TaskCanceledException error)
    {
        Console.WriteLine($"Failed to download from {url}: {error.Message}");
    }
}
Console.WriteLine($"Downloaded {images.Count} images");
if (images.Count == 0)
{
    throw new InvalidOperationException("No valid images found.");
}

foreach (var (monitor, image) in monitors.Zip(images))
{
    Console.WriteLine($"Setting wallpaper for {monitor} to {image}");
    desktop.SetWallpaper(monitor, image);
}
Console.WriteLine($"Set wallpaper for {images.Count} monitors");

internal sealed record class ApodEntry
{
    [JsonPropertyName("date")]
    public required string Date { get; init; }

    [JsonPropertyName("media_type")]
    public string? MediaType { get; init; }

    [JsonPropertyName("hdurl")]
    public string? HdUrl { get; init; }
}

[JsonSerializable(typeof(List<ApodEntry>))]
internal sealed partial class ApodApiJsonContext : JsonSerializerContext
{
}

// Only the first four COM methods are needed. Keep their native vtable order.
// Without PreserveSig, .NET converts a failed HRESULT into a COM exception.
[ComImport]
[Guid("B92B56A9-8B55-4E14-9A89-0199BBB6F93B")]
[InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
internal interface IDesktopWallpaper
{
    void SetWallpaper([MarshalAs(UnmanagedType.LPWStr)] string monitorID, [MarshalAs(UnmanagedType.LPWStr)] string wallpaper);
    void GetWallpaper([MarshalAs(UnmanagedType.LPWStr)] string monitorID, [MarshalAs(UnmanagedType.LPWStr)] out string wallpaper);
    void GetMonitorDevicePathAt(uint monitorIndex, [MarshalAs(UnmanagedType.LPWStr)] out string monitorID);
    void GetMonitorDevicePathCount(out uint count);
}

[ComImport, Guid("C2CF3110-460E-4fc1-B9D0-8A1C0C9CC4BD")]
internal sealed class DesktopWallpaperClass
{
}
