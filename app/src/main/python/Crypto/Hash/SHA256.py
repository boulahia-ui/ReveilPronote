import hashlib

digest_size = hashlib.new("sha256").digest_size


def new(data=None):
    h = hashlib.new("sha256")
    if data is not None:
        h.update(bytes(data))
    return h
