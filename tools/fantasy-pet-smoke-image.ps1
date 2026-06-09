function Assert-SmokeImageDecodes {
    param(
        [byte[]]$Bytes,
        [string]$Label = "image"
    )

    if ($null -eq $Bytes -or $Bytes.Length -le 0) {
        throw "$Label returned no bytes"
    }

    Add-Type -AssemblyName System.Drawing
    if ($null -eq ("GamerFantasyPetSmokeImageValidator" -as [type])) {
        Add-Type -ReferencedAssemblies "System.Drawing" -TypeDefinition @'
using System;
using System.Drawing;
using System.IO;
using System.Text;

public sealed class GamerFantasyPetSmokeImageInfo
{
    public int width { get; set; }
    public int height { get; set; }
}

public static class GamerFantasyPetSmokeImageValidator
{
    private static readonly byte[] PngSignature = new byte[] { 137, 80, 78, 71, 13, 10, 26, 10 };

    public static GamerFantasyPetSmokeImageInfo Decode(byte[] bytes, string label)
    {
        if (bytes == null || bytes.Length == 0)
        {
            throw new InvalidDataException(label + " returned no bytes");
        }

        if (LooksLikePng(bytes))
        {
            ValidatePngChunks(bytes, label);
        }

        using (MemoryStream stream = new MemoryStream(bytes, false))
        using (Image image = Image.FromStream(stream, true, true))
        {
            if (image.Width <= 0 || image.Height <= 0)
            {
                throw new InvalidDataException(label + " decoded with invalid dimensions");
            }

            return new GamerFantasyPetSmokeImageInfo
            {
                width = image.Width,
                height = image.Height
            };
        }
    }

    private static bool LooksLikePng(byte[] bytes)
    {
        if (bytes.Length < PngSignature.Length)
        {
            return false;
        }

        for (int index = 0; index < PngSignature.Length; index++)
        {
            if (bytes[index] != PngSignature[index])
            {
                return false;
            }
        }

        return true;
    }

    private static void ValidatePngChunks(byte[] bytes, string label)
    {
        int offset = PngSignature.Length;
        bool sawIhdr = false;
        bool sawIend = false;

        while (offset < bytes.Length)
        {
            if (offset + 12 > bytes.Length)
            {
                throw new InvalidDataException(label + " PNG is truncated");
            }

            uint length = ReadBigEndianUInt32(bytes, offset);
            offset += 4;
            if (length > int.MaxValue)
            {
                throw new InvalidDataException(label + " PNG chunk is too large");
            }

            int typeOffset = offset;
            offset += 4;

            int dataLength = (int)length;
            if (offset + dataLength + 4 > bytes.Length)
            {
                throw new InvalidDataException(label + " PNG chunk is truncated");
            }

            offset += dataLength;
            uint storedCrc = ReadBigEndianUInt32(bytes, offset);
            offset += 4;
            uint actualCrc = ComputeCrc32(bytes, typeOffset, 4 + dataLength);
            string chunkType = Encoding.ASCII.GetString(bytes, typeOffset, 4);

            if (storedCrc != actualCrc)
            {
                throw new InvalidDataException(label + " PNG chunk CRC mismatch in " + chunkType);
            }

            if (chunkType == "IHDR")
            {
                sawIhdr = true;
            }
            else if (chunkType == "IEND")
            {
                sawIend = true;
                break;
            }
        }

        if (!sawIhdr)
        {
            throw new InvalidDataException(label + " PNG is missing IHDR");
        }

        if (!sawIend)
        {
            throw new InvalidDataException(label + " PNG is missing IEND");
        }
    }

    private static uint ReadBigEndianUInt32(byte[] bytes, int offset)
    {
        return ((uint)bytes[offset] << 24)
            | ((uint)bytes[offset + 1] << 16)
            | ((uint)bytes[offset + 2] << 8)
            | bytes[offset + 3];
    }

    private static uint ComputeCrc32(byte[] bytes, int offset, int count)
    {
        uint crc = 0xffffffffu;
        for (int index = offset; index < offset + count; index++)
        {
            crc ^= bytes[index];
            for (int bit = 0; bit < 8; bit++)
            {
                if ((crc & 1u) == 1u)
                {
                    crc = (crc >> 1) ^ 0xedb88320u;
                }
                else
                {
                    crc >>= 1;
                }
            }
        }

        return crc ^ 0xffffffffu;
    }
}
'@
    }

    try {
        [GamerFantasyPetSmokeImageValidator]::Decode($Bytes, $Label)
    } catch {
        throw "$Label is not a decodable image: $($_.Exception.Message)"
    }
}
