$(function () {
    (document.querySelectorAll('.notification .delete') || []).forEach(($delete) => {
        var $notification = $delete.parentNode;
        $delete.addEventListener('click', () => {
            debugger;
            $notification.parentNode.removeChild($notification);
        });
    });
});

