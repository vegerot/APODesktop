from PIL import Image, ImageDraw
import os

res_root = "/home/max/workspace/github.com/vegerot/apodesktop/android/app/src/main/res"
original_icon_path = "/home/max/.gemini/antigravity/brain/0702bf4b-5b2b-4ab6-83b2-5bfd2367e3cb/apod_app_icon_1780984382092.png"
bg_path = "/home/max/.gemini/antigravity/brain/0702bf4b-5b2b-4ab6-83b2-5bfd2367e3cb/apod_icon_background_1780984723120.png"

# Densities and sizes (canvas_size, safe_zone_size, legacy_size)
densities = {
    "mipmap-mdpi": (108, 76, 48),
    "mipmap-hdpi": (162, 114, 72),
    "mipmap-xhdpi": (216, 152, 96),
    "mipmap-xxhdpi": (324, 228, 144),
    "mipmap-xxxhdpi": (432, 304, 192)
}

print("Cropping the glassmorphic squircle from the original icon...")
# Load original icon and convert to RGBA
src_img = Image.open(original_icon_path).convert("RGBA")
width, height = src_img.size  # Should be 1024x1024

# Create a squircle mask to remove the outer dark gray border
# Bounding box is centered from 132 to 892 (width 760)
mask = Image.new("L", (width, height), 0)
draw = ImageDraw.Draw(mask)
draw.rounded_rectangle((132, 132, 892, 892), radius=190, fill=255)

# Apply mask to make everything outside the squircle transparent
cropped_fg = Image.new("RGBA", (width, height), (0, 0, 0, 0))
cropped_fg.paste(src_img, (0, 0), mask)

# Load background nebula
bg_img = Image.open(bg_path).convert("RGBA")

# Helper to create a circular mask
def create_circular_mask(size):
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size, size), fill=255)
    return mask

# Generate all assets
for folder, (canvas_sz, safe_sz, legacy_sz) in densities.items():
    dest_dir = os.path.join(res_root, folder)
    os.makedirs(dest_dir, exist_ok=True)
    
    # 1. Resize and save background layer (nebula texture)
    bg_resized = bg_img.resize((canvas_sz, canvas_sz), Image.Resampling.LANCZOS)
    bg_resized.save(os.path.join(dest_dir, "ic_launcher_background.webp"), "WEBP")
    
    # 2. Resize and center foreground layer (the cropped glassmorphic squircle)
    # The squircle itself serves as the foreground floating on the background
    fg_resized = cropped_fg.resize((safe_sz, safe_sz), Image.Resampling.LANCZOS)
    fg_canvas = Image.new("RGBA", (canvas_sz, canvas_sz), (0, 0, 0, 0))
    offset = (canvas_sz - safe_sz) // 2
    fg_canvas.paste(fg_resized, (offset, offset), fg_resized)
    fg_canvas.save(os.path.join(dest_dir, "ic_launcher_foreground.webp"), "WEBP")
    
    # 3. Create legacy flat icon (the glassmorphic squircle on transparent background)
    # Since legacy devices don't force shapes, we can just save the cropped squircle directly
    legacy_square = cropped_fg.resize((legacy_sz, legacy_sz), Image.Resampling.LANCZOS)
    legacy_square.save(os.path.join(dest_dir, "ic_launcher.webp"), "WEBP")
    
    # 4. Create legacy round icon
    # Since legacy round expects a filled circle, we paste the squircle onto the nebula and apply a circular mask
    flat_canvas = bg_resized.copy()
    flat_canvas.paste(fg_canvas, (0, 0), fg_canvas)
    round_mask = create_circular_mask(legacy_sz)
    legacy_round = Image.new("RGBA", (legacy_sz, legacy_sz), (0, 0, 0, 0))
    legacy_round.paste(flat_canvas.resize((legacy_sz, legacy_sz), Image.Resampling.LANCZOS), (0, 0), round_mask)
    legacy_round.save(os.path.join(dest_dir, "ic_launcher_round.webp"), "WEBP")
    
    print(f"Generated assets for {folder}")

print("All glassmorphic icons successfully generated!")
