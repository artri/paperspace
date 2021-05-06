function DocumentController(config) {
    const configuration = config;

    this.init = function () {
        let doneButton = document.querySelector(configuration.doneButton);
        if (doneButton) {
            $(configuration.doneButton).on('click', (e) => {
                $(doneButton).toggleClass('is-loading');
                $.post(configuration.doneUrl)
                    .done(() => location.reload())
                    .fail((error) => {
                        console.error(error);
                    }).always(() => {
                    $(doneButton).toggleClass('is-loading');
                });
            })
        }

        let deleteButton = document.querySelector(configuration.deleteButton);
        if (deleteButton) {
            $(configuration.deleteButton).on('click', (e) => {
                $(deleteButton).toggleClass('is-loading');
                $.ajax({
                    url: configuration.deleteUrl,
                    type: 'DELETE'
                }).done(() => window.location = configuration.appUrl)
                    .fail((error) => {
                        console.error(error);
                    }).always(() => {
                    $(deleteButton).toggleClass('is-loading');
                });
            })
        }
        const saveButton = $(configuration.saveButton);
        saveButton.on('click', (e) => {
            saveButton.toggleClass('is-loading');
            e.preventDefault();
            $.post(configuration.selfUrl, $(configuration.form).serialize())
                .done(() => location.reload())
                .fail((error) => {
                    console.error(error);
                }).always(() => {
                saveButton.toggleClass('is-loading');
            });
            return false;
        })
        $('#tags-with-source').select2({
            placeholder: 'Search or create tags',
            tags: true,
            tokenSeparators: [',', ' '],
            ajax: {
                url: '/api/tags.json',
                data: function (params) {
                    return {
                        search: params.term
                    };
                },
                processResults: function (data) {
                    data = $.map(data, function (obj) {
                        obj.text = obj.text || obj.name;
                        obj.id = obj.name;
                        return obj;
                    });
                    return {
                        results: data
                    };
                }
            },
            createTag: function (params) {
                const term = $.trim(params.term);
                if (term === '') {
                    return null;
                }

                return {
                    id: term,
                    text: term,
                    isNew: true
                }
            }
        });
    }
}
