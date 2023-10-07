package com.shrifd.ls149.entity

/**
 * author: 万涛
 * 2021-03-16 15:32
 */
class FlowPeople (var toDayEnter:Int,var toDayOut:Int
,var totalEnter:Int,var totalOut:Int)
{


    constructor() : this(0,0,0,0) {

    }


    override fun toString(): String {
        return "FlowPeople(toDayEnter=$toDayEnter, toDayOut=$toDayOut, totalEnter=$totalEnter, totalOut=$totalOut)"
    }
}