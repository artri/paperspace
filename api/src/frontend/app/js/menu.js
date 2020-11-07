$(function () {
    'use strict'

    $('[data-toggle="side-navigation"]').on('click', function () {
        const navigationId = $(this).data('target');
        $(this).toggleClass('is-active')
        $(`#${navigationId}`).toggleClass('is-hidden-touch');
    })
})