import os
import re

path = r'c:\Users\tmddj\Desktop\project\malang_key\florisboard\app\src\main\assets\ime\theme\org.florisboard.themes\stylesheets'
files = [f for f in os.listdir(path) if f.endswith('.json')]

for file in files:
    file_path = os.path.join(path, file)
    with open(file_path, 'r', encoding='utf-8') as f:
        data = f.read()
    
    # We want to replace var(--surface) with var(--background) and var(--on-surface) with var(--on-background)
    # inside clipboard-item-popup and clipboard-item-actions.
    
    data = re.sub(r'("clipboard-item-popup":\s*\{[^\}]+?background":\s*")var\(--surface\)(")', r'\g<1>var(--background)\g<2>', data)
    data = re.sub(r'("clipboard-item-popup":\s*\{[^\}]+?foreground":\s*")var\(--on-surface\)(")', r'\g<1>var(--on-background)\g<2>', data)
    
    data = re.sub(r'("clipboard-item-actions":\s*\{[^\}]+?background":\s*")var\(--surface\)(")', r'\g<1>var(--background)\g<2>', data)
    data = re.sub(r'("clipboard-item-actions":\s*\{[^\}]+?foreground":\s*")var\(--on-surface\)(")', r'\g<1>var(--on-background)\g<2>', data)

    # Some themes use --primary-container instead of --surface
    data = re.sub(r'("clipboard-item-popup":\s*\{[^\}]+?background":\s*")var\(--primary-container\)(")', r'\g<1>var(--background)\g<2>', data)
    data = re.sub(r'("clipboard-item-popup":\s*\{[^\}]+?foreground":\s*")var\(--on-primary-container\)(")', r'\g<1>var(--on-background)\g<2>', data)

    data = re.sub(r'("clipboard-item-actions":\s*\{[^\}]+?background":\s*")var\(--primary-container\)(")', r'\g<1>var(--background)\g<2>', data)
    data = re.sub(r'("clipboard-item-actions":\s*\{[^\}]+?foreground":\s*")var\(--on-primary-container\)(")', r'\g<1>var(--on-background)\g<2>', data)

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(data)
    print(f"Updated {file}")
