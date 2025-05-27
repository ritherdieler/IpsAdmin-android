package com.dscorp.ispadmin.di

interface ResourceProvider {
    fun getString(resourceId: Int): String
}