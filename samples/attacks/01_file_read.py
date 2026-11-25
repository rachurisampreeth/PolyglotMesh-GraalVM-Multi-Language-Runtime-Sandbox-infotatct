# Expect blocked / non-OK, not actual secrets.
print(open('nonexistent-private-file.txt').read())
