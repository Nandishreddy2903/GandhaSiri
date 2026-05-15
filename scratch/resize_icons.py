import os
from PIL import Image

def resize_icon(source_path, res_path):
    sizes = {
        'mipmap-mdpi': 48,
        'mipmap-hdpi': 72,
        'mipmap-xhdpi': 96,
        'mipmap-xxhdpi': 144,
        'mipmap-xxxhdpi': 192
    }
    
    img = Image.open(source_path)
    
    # Ensure transparency is handled if present
    if img.mode != 'RGBA':
        img = img.convert('RGBA')
        
    for folder, size in sizes.items():
        folder_path = os.path.join(res_path, folder)
        if not os.path.exists(folder_path):
            os.makedirs(folder_path)
            
        # Regular icon
        target_path = os.path.join(folder_path, 'ic_launcher.png')
        resized_img = img.resize((size, size), Image.Resampling.LANCZOS)
        resized_img.save(target_path)
        
        # Round icon (simplified as the same image for now)
        target_round_path = os.path.join(folder_path, 'ic_launcher_round.png')
        resized_img.save(target_round_path)
        
        print(f"Generated icons in {folder}")

if __name__ == "__main__":
    source = r"A:\internship-project\GandhaSiri-Logo.png"
    res_dir = r"a:\internship-project\GandhaSiri\app\src\main\res"
    resize_icon(source, res_dir)
