class RsaKey:
    def __init__(self, n, e):
        self.n = int(n)
        self.e = int(e)

    def size_in_bits(self):
        return self.n.bit_length()

    def has_private(self):
        return False


def construct(rsa_components, consistency_check=True):
    n, e = rsa_components[0], rsa_components[1]
    return RsaKey(n, e)
