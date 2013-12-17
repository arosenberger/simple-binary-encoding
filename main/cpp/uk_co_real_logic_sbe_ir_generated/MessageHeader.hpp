/* Generated SBE (Simple Binary Encoding) message codec */
#ifndef _MESSAGEHEADER_HPP_
#define _MESSAGEHEADER_HPP_

/* math.h needed for NAN */
#include <math.h>
#include "sbe/sbe.hpp"

using namespace sbe;

namespace uk_co_real_logic_sbe_ir_generated {

class MessageHeader
{
private:
    char *buffer_;
    int offset_;
    int actingVersion_;

public:
    MessageHeader &wrap(char *buffer, const int offset, const int actingVersion)
    {
        buffer_ = buffer;
        offset_ = offset;
        actingVersion_ = actingVersion;
        return *this;
    }

    static int size(void)
    {
        return 6;
    }


    static sbe_uint16_t blockLengthNullVal()
    {
        return (sbe_uint16_t)65535;
    }

    static sbe_uint16_t blockLengthMinVal()
    {
        return (sbe_uint16_t)0;
    }

    static sbe_uint16_t blockLengthMaxVal()
    {
        return (sbe_uint16_t)65534;
    }

    sbe_uint16_t blockLength(void) const
    {
        return SBE_LITTLE_ENDIAN_ENCODE_16(*((sbe_uint16_t *)(buffer_ + offset_ + 0)));
    }

    MessageHeader &blockLength(const sbe_uint16_t value)
    {
        *((sbe_uint16_t *)(buffer_ + offset_ + 0)) = SBE_LITTLE_ENDIAN_ENCODE_16(value);
        return *this;
    }

    static sbe_uint16_t templateIdNullVal()
    {
        return (sbe_uint16_t)65535;
    }

    static sbe_uint16_t templateIdMinVal()
    {
        return (sbe_uint16_t)0;
    }

    static sbe_uint16_t templateIdMaxVal()
    {
        return (sbe_uint16_t)65534;
    }

    sbe_uint16_t templateId(void) const
    {
        return SBE_LITTLE_ENDIAN_ENCODE_16(*((sbe_uint16_t *)(buffer_ + offset_ + 2)));
    }

    MessageHeader &templateId(const sbe_uint16_t value)
    {
        *((sbe_uint16_t *)(buffer_ + offset_ + 2)) = SBE_LITTLE_ENDIAN_ENCODE_16(value);
        return *this;
    }

    static sbe_uint8_t versionNullVal()
    {
        return (sbe_uint8_t)255;
    }

    static sbe_uint8_t versionMinVal()
    {
        return (sbe_uint8_t)0;
    }

    static sbe_uint8_t versionMaxVal()
    {
        return (sbe_uint8_t)254;
    }

    sbe_uint8_t version(void) const
    {
        return (*((sbe_uint8_t *)(buffer_ + offset_ + 4)));
    }

    MessageHeader &version(const sbe_uint8_t value)
    {
        *((sbe_uint8_t *)(buffer_ + offset_ + 4)) = (value);
        return *this;
    }

    static sbe_uint8_t reservedNullVal()
    {
        return (sbe_uint8_t)255;
    }

    static sbe_uint8_t reservedMinVal()
    {
        return (sbe_uint8_t)0;
    }

    static sbe_uint8_t reservedMaxVal()
    {
        return (sbe_uint8_t)254;
    }

    sbe_uint8_t reserved(void) const
    {
        return (*((sbe_uint8_t *)(buffer_ + offset_ + 5)));
    }

    MessageHeader &reserved(const sbe_uint8_t value)
    {
        *((sbe_uint8_t *)(buffer_ + offset_ + 5)) = (value);
        return *this;
    }
};
}
#endif
