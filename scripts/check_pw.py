import subprocess
result = subprocess.run(
    ['docker', 'exec', 'mes-mysql-primary', 'mysql', '-u', 'root', '-p12345678', 'mes', '-N', '-e',
     "SELECT password FROM sys_user WHERE username='admin';"],
    capture_output=True, text=True
)
pw_hash = result.stdout.strip()
print(f"Hash: {pw_hash}")

try:
    import bcrypt
    ok = bcrypt.checkpw(b'123456', pw_hash.encode('utf-8'))
    print(f"'123456' matches: {ok}")
    ok2 = bcrypt.checkpw(b'admin123', pw_hash.encode('utf-8'))
    print(f"'admin123' matches: {ok2}")
except ImportError:
    print("bcrypt not installed, trying with Spring BCrypt via Java...")
