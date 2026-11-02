from Crypto import _java


class _Pkcs1Cipher:
    def __init__(self, key):
        self._key = key

    def encrypt(self, message):
        spec = _java.RSAPublicKeySpec(
            _java.BigInteger(format(self._key.n, "x"), 16),
            _java.BigInteger(format(self._key.e, "x"), 16),
        )
        public_key = _java.KeyFactory.getInstance("RSA").generatePublic(spec)
        cipher = _java.Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(_java.Cipher.ENCRYPT_MODE, public_key)
        return _java.to_bytes(cipher.doFinal(bytes(message)))

    def can_encrypt(self):
        return True


def new(key, randfunc=None):
    return _Pkcs1Cipher(key)
