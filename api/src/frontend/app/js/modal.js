$(function () {
    'use strict'

    $('[data-toggle="modal"]').on('click', function () {
        $($(this).data('target')).toggleClass('is-active');
        return false;
    })
});