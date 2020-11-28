function ErrorController(config, callbacks) {
    const configuration = config;
    let lastErrors = 0;

    function loadStatus() {
        const errorContainer = $(config.errorLabel);

        $.get('/api/errors.json').done(data => {
            if (data.length === 0) {
                errorContainer.hide();
            } else if (data.length !== lastErrors) {
                $(errorContainer).innerHTML = ``;
                for (const file of data) {
                    $(errorContainer).append(`
                        <div class="notification is-danger is-active columns mt-1">
                            <p class="column is-8">
                                processing of <strong>${file.path}</strong> failed
                            </p>
                            <p class="column is-4 has-text-right">
                                <button data-href="${file.links.ignore}" class="button is-small is-warning" title="ignore file">
                                    <span class="icon is-small"><i class="fas fa-eye-slash"></i></span>
                                </button>
                                <button data-href="${file.links.delete}" class="button is-small is-danger is-light" title="delete file">
                                    <span class="icon is-small"><i class="fa fa-trash"></i></span>
                                </button>
                            </p>
                        </div>`);
                }
                errorContainer.show();
                $('button', errorContainer).on('click', function () {
                   const href = $(this).data('href');
                   $.post(href).done(() => {loadStatus()})
                });
            }

            lastErrors = data.length;
        });
    }

    this.init = function () {
        window.setInterval(function () {
            loadStatus();
        }, 5000)
        loadStatus();
    }

}