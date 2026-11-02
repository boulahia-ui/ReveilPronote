import hashlib

digest_size = hashlib.new("md5").digest_size


def new(data=None):
    h = hashlib.new("md5")
    if data is not None:
        h.update(bytes(data))
    return h
