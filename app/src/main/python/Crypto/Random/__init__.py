import os


def get_random_bytes(n):
    return os.urandom(n)


def new(*args, **kwargs):
    class _R:
        def read(self, n):
            return os.urandom(n)
    return _R()
