package com.supercoolapps.models

enum class BoardSize(val numCards:Int) {
    EASY(8),
    MEDIUM(18),
    HARD(24);

    companion object{
        fun getbyValue(value: Int) = values().first{it.numCards == value}
    }

    fun getWidth(): Int{
        return when(this){
            EASY -> 2
            MEDIUM -> 3
            HARD -> 4
        }
    }

    fun getHeight(): Int = numCards / getWidth()

    fun getNumPairs():Int = numCards / 2
}