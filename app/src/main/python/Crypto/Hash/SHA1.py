import hashlib

digest_size = hashlib.new("sha1").digest_size


def new(data=None):
    h = hashlib.new("sha1")
    if data is not None:
        h.update(bytes(data))
    return h
