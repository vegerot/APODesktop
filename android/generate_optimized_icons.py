from PIL import Image, ImageDraw
import os
import shutil

res_root = "/home/max/workspace/github.com/vegerot/apodesktop/android/app/src/main/res"
original_icon_path = "/home/max/.gemini/antigravity/brain/0702bf4b-5b2b-4ab6-83b2-5bfd2367e3cb/apod_app_icon_1780984382092.png"
bg_path = "/home/max/.gemini/antigravity/brain/0702bf4b-5b2b-4ab6-83b2-5bfd2367e3cb/apod_icon_background_1780984723120.png"

# Target directories
drawable_nodpi_dir = os.path.join(res_root, "drawable-nodpi")
mipmap_nodpi_dir = os.path.join(res_root, "mipmap-nodpi")

os.makedirs(drawable_nodpi_dir, exist_ok=True)
os.makedirs(mipmap_nodpi_dir, exist_ok=True)

# Helper to create a circular mask
def create_circular_mask(size):
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size, size), fill=255)
    return mask

# 1. Process Foreground (Crop squircle, make background transparent)
print("Processing foreground icon...")
src_img = Image.open(original_icon_path).convert("RGBA")
width, height = src_img.size  # 1024x1024

mask = Image.new("L", (width, height), 0)
draw = ImageDraw.Draw(mask)
draw.rounded_rectangle((132, 132, 892, 892), radius=190, fill=255)

cropped_fg = Image.new("RGBA", (width, height), (0, 0, 0, 0))
cropped_fg.paste(src_img, (0, 0), mask)

# 2. Load Background
bg_img = Image.open(bg_path).convert("RGBA")

# 3. Generate adaptive layers (xxxhdpi size: 432x432 px, safe zone: 304x304 px)
print("Generating adaptive layers (432x432 px)...")
# Background layer
bg_resized = bg_img.resize((432, 432), Image.Resampling.LANCZOS)
bg_resized.save(os.path.join(drawable_nodpi_dir, "ic_launcher_background.webp"), "WEBP")

# Foreground layer (centered)
fg_resized = cropped_fg.resize((304, 304), Image.Resampling.LANCZOS)
fg_canvas = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
fg_canvas.paste(fg_resized, (64, 64), fg_resized)
fg_canvas.save(os.path.join(drawable_nodpi_dir, "ic_launcher_foreground.webp"), "WEBP")

# 4. Generate legacy fallback icons (xxxhdpi size: 192x192 px)
print("Generating legacy fallbacks (192x192 px)...")
# Square legacy
legacy_square = cropped_fg.resize((192, 192), Image.Resampling.LANCZOS)
legacy_square.save(os.path.join(mipmap_nodpi_dir, "ic_launcher.webp"), "WEBP")

# Round legacy
flat_canvas = bg_resized.copy()
flat_canvas.paste(fg_canvas, (0, 0), fg_canvas)
round_mask = create_circular_mask(192)
legacy_round = Image.new("RGBA", (192, 192), (0, 0, 0, 0))
legacy_round.paste(flat_canvas.resize((192, 192), Image.Resampling.LANCZOS), (0, 0), round_mask)
legacy_round.save(os.path.join(mipmap_nodpi_dir, "ic_launcher_round.webp"), "WEBP")

# 5. Delete redundant density-specific mipmap folders
old_folders = ["mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi"]
for folder in old_folders:
    dir_to_remove = os.path.join(res_root, folder)
    if os.path.exists(dir_to_remove):
        shutil.rmtree(dir_to_remove)
        print(f"Removed old folder: {folder}")

print("Clean adaptive icon generation completed successfully!")
