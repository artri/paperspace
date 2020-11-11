
function StatusController(config, callbacks) {
    const configuration = config;
    const statusUpdateCallbacks = callbacks;
    let currentPendingChanges = 0;
    const toastTemplate = `
    <div id="solrReindexNotification" class="notification is-danger is-fixed">
        <button class="delete"></button>
        <p>Stored documents have to be updated that the search can work correct.</p>
        <p class="mt-2">
            <button class="button is-upload" id="solrReindexButton">update</button>
        </p>
    </div>`;

    function updatePendingFileChanges(pendingChanges) {
        const isShown = $('span', configuration.pendingFilePanel).length !== 0;
        if (pendingChanges !== 0) {
            if (!isShown) {
                $(configuration.pendingFilePanel).html(`<span class="blinking"></span>`);
            }
            $('span', configuration.pendingFilePanel).text(`text recognition pending: ${pendingChanges}`);
        } else {
            $(configuration.pendingFilePanel).html('');
        }
        if (statusUpdateCallbacks && currentPendingChanges !== 0 && pendingChanges !== currentPendingChanges) {
            for (const statusUpdateCallback of statusUpdateCallbacks) {
                statusUpdateCallback.changed('files');
            }
        }
        currentPendingChanges = pendingChanges;
    }

    function loadStatus() {
        $.get('/api/status.json').done(data => {
            updatePendingFileChanges(data.pendingChanges)
            if (data.data === 'NEEDS_UPGRADE') {
                showReindexNotification();
            } else {
                $('#solrReindexNotification').remove()
            }
        });
    }

    this.init = function () {
        window.setInterval(function () {
            loadStatus();
        }, 5000)
        loadStatus();
    }

    function showReindexNotification() {
        const alreadyShown = $('#solrReindexNotification').length === 1;
        if (!alreadyShown) {
            $(toastTemplate).appendTo(document.body);

            const reindexButton = document.getElementById('solrReindexButton');
            const $delete = document.querySelector('.notification .delete');
            const $notification = $delete.parentNode;

            $delete.addEventListener('click', () => {
                $notification.parentNode.removeChild($notification);
            });

            $('#solrReindexButton').on('click', ev => {
                $('#solrReindexButton').toggleClass('is-loading');
                $.post('/api/reindex')
                    .done(() => {
                        reindexButton.setAttribute('disabled', 'disabled');
                        reindexButton.textContent = 'Update finished.'
                        window.setTimeout(() => $('#solrReindexNotification').remove());
                    })
                    .always(() => {
                        $('#solrReindexButton').toggleClass('is-loading');
                    })
            })
        }
    }
}