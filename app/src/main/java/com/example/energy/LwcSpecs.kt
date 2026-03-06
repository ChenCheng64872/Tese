package com.example.energy.lwc

object LwcSpecs {
    val ASCON_128 = AeadSpec("ASCON-AEAD128", keyBytes = 16, nonceBytes = 16, tagBits = 128)
    val ELEPHANT  = AeadSpec("Elephant",       keyBytes = 16, nonceBytes = 12, tagBits = 128)
    val GIFT_COFB = AeadSpec("GIFT-COFB",      keyBytes = 16, nonceBytes = 16, tagBits = 128)
    val GRAIN128  = AeadSpec("Grain-128AEAD",  keyBytes = 16, nonceBytes = 12, tagBits = 128)
    val XOODYAK   = AeadSpec("Xoodyak",        keyBytes = 16, nonceBytes = 16, tagBits = 128)
}