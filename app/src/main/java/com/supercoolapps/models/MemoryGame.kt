package com.supercoolapps.models

import com.supercoolapps.utils.DEFAULT_ICONS

class MemoryGame(private val boardSize: BoardSize, private val customGameImages: List<String>?) {

    val cards : List<MemoryCard>
    var numOfPairs:Int = 0

    private var numCardFlips = 0
    private var indexOfSingleSelectedCard: Int? = null

    init {
        if(customGameImages == null){
            var chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
            val randomizedImages  = (chosenImages+ chosenImages).shuffled()
            cards = randomizedImages.map { MemoryCard(it) }
        }else{
            val randomizedImages = (customGameImages + customGameImages).shuffled()
            cards = randomizedImages.map { MemoryCard(it.hashCode(), it) }
        }
    }
    fun flipCard(position: Int): Boolean {
        numCardFlips++
        var card = cards[position]
        var foundMatch = false
        if(indexOfSingleSelectedCard == null){
            restoreCards()
            indexOfSingleSelectedCard = position
        }else{
            foundMatch = checkForMatch(indexOfSingleSelectedCard!!, position)
            indexOfSingleSelectedCard = null
        }
        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(indexOfSingleSelectedCard: Int, position: Int): Boolean{
        if(cards[indexOfSingleSelectedCard].identifier != cards[position].identifier) return false

        cards[indexOfSingleSelectedCard].isMatched = true
        cards[position].isMatched = true
        numOfPairs++
        return true
    }

    private fun restoreCards() {
       for(card in cards){
           if(!card.isMatched){
               card.isFaceUp = false
           }
       }
    }

    fun haveWonGame(): Boolean  = numOfPairs == boardSize.getNumPairs()

    fun isCardFaceUp(position: Int): Boolean  =  cards[position].isFaceUp

    fun getNumMoves(): Int = numCardFlips / 2
}