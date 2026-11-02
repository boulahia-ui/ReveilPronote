from Crypto import _java

MODE_ECB = 1
MODE_CBC = 2
block_size = 16
key_size = (16, 24, 32)


class _CbcCipher:
    def __init__(self, key, iv):
        self._key = bytes(key)
        self._iv = bytes(iv) if iv is not None else bytes(16)
        if len(self._key) not in key_size:
            raise ValueError("Incorrect AES key length (%d bytes)" % len(self._key))
        if len(self._iv) != 16:
            raise ValueError("Incorrect IV length (it must be 16 bytes long)")
        self.iv = self._iv

    def _run(self, mode, data):
        data = bytes(data)
        if len(data) % 16:
            raise ValueError("Data must be padded to 16 byte boundary in CBC mode")
        if not data:
            return b""
        try:
            cipher = _java.Cipher.getInstance("AES/CBC/NoPadding")
            cipher.init(mode, _java.SecretKeySpec(self._key, "AES"), _java.IvParameterSpec(self._iv))
            out = _java.to_bytes(cipher.doFinal(data))
        except Exception as e:  # erreurs Java → ValueError, comme pycryptodome
            raise ValueError(str(e))
        # Chaînage CBC entre appels successifs, comme pycryptodome
        self._iv = (out if mode == _java.Cipher.ENCRYPT_MODE else data)[-16:]
        return out

    def encrypt(self, plaintext):
        return self._run(_java.Cipher.ENCRYPT_MODE, plaintext)

    def decrypt(self, ciphertext):
        return self._run(_java.Cipher.DECRYPT_MODE, ciphertext)


def new(key, mode, iv=None, *args, **kwargs):
    if iv is None:
        iv = kwargs.get("IV")
    if mode != MODE_CBC:
        raise ValueError("Seul le mode AES-CBC est pris en charge")
    return _CbcCipher(key, iv)
