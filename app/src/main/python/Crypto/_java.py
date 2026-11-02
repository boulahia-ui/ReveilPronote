from java import jclass

Cipher = jclass("javax.crypto.Cipher")
SecretKeySpec = jclass("javax.crypto.spec.SecretKeySpec")
IvParameterSpec = jclass("javax.crypto.spec.IvParameterSpec")
KeyFactory = jclass("java.security.KeyFactory")
RSAPublicKeySpec = jclass("java.security.spec.RSAPublicKeySpec")
BigInteger = jclass("java.math.BigInteger")


def to_bytes(java_array):
    """Convertit un byte[] Java en bytes Python."""
    try:
        return bytes(java_array)
    except Exception:
        return bytes((b & 0xFF) for b in java_array)
