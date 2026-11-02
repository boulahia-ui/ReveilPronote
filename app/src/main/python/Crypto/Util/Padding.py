def pad(data_to_pad, block_size, style="pkcs7"):
    n = block_size - len(data_to_pad) % block_size
    return bytes(data_to_pad) + bytes([n]) * n


def unpad(padded_data, block_size, style="pkcs7"):
    data = bytes(padded_data)
    if not data or len(data) % block_size:
        raise ValueError("Input data is not padded")
    n = data[-1]
    if n < 1 or n > block_size or data[-n:] != bytes([n]) * n:
        raise ValueError("Padding is incorrect.")
    return data[:-n]
