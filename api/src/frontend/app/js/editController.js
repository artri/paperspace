function EditController(config) {
    const editPanelTemplate = Handlebars.compile(`
    {{#each .}}
     <div class="item column is-one-third" data-index="{{@index}}">
        <figure class="image is-1by1 {{page.style}}">
            <img src="{{page.links.preview}}" alt="">
        </figure>
        <div class="page-name">{{page.name}}</div>
        <div class="document-edit-bar">
            <button data-index="{{@index}}" class="button" {{#if @first}}disabled="disabled"{{/if}} title="move left" data-action="move-left">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-chevron-left" viewBox="0 0 16 16">
                    <path fill-rule="evenodd" d="M11.354 1.646a.5.5 0 0 1 0 .708L5.707 8l5.647 5.646a.5.5 0 0 1-.708.708l-6-6a.5.5 0 0 1 0-.708l6-6a.5.5 0 0 1 .708 0z"/>
                </svg>
            </button>
            <button data-index="{{@index}}" class="button" title="rotate 90deg counter clockwise" data-action="rotate-counterclockwise">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-arrow-counterclockwise" viewBox="0 0 16 16">
                    <path fill-rule="evenodd" d="M8 3a5 5 0 1 1-4.546 2.914.5.5 0 0 0-.908-.417A6 6 0 1 0 8 2v1z"/>
                    <path d="M8 4.466V.534a.25.25 0 0 0-.41-.192L5.23 2.308a.25.25 0 0 0 0 .384l2.36 1.966A.25.25 0 0 0 8 4.466z"/>
                </svg>
            </button>
            <button data-index="{{@index}}" class="button" title="delete" data-action="delete">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-file-earmark-x" viewBox="0 0 16 16">
                    <path d="M6.854 7.146a.5.5 0 1 0-.708.708L7.293 9l-1.147 1.146a.5.5 0 0 0 .708.708L8 9.707l1.146 1.147a.5.5 0 0 0 .708-.708L8.707 9l1.147-1.146a.5.5 0 0 0-.708-.708L8 8.293 6.854 7.146z"/>
                    <path d="M14 14V4.5L9.5 0H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2zM9.5 3A1.5 1.5 0 0 0 11 4.5h2V14a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1h5.5v2z"/>
                </svg>
            </button>
            <button data-index="{{@index}}" class="button" title="rotate 90deg clockwise" data-action="rotate-clockwise">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-arrow-clockwise" viewBox="0 0 16 16">
                    <path fill-rule="evenodd" d="M8 3a5 5 0 1 0 4.546 2.914.5.5 0 0 1 .908-.417A6 6 0 1 1 8 2v1z"/>
                    <path d="M8 4.466V.534a.25.25 0 0 1 .41-.192l2.36 1.966c.12.1.12.284 0 .384L8.41 4.658A.25.25 0 0 1 8 4.466z"/>
                </svg>
            </button>
            <button data-index="{{@index}}" {{#if @last}}disabled="disabled"{{/if}} class="button"  title="move right" data-action="move-right">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-chevron-right" viewBox="0 0 16 16">
                    <path fill-rule="evenodd" d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708z"/>
                </svg>
            </button>
        </div>
    </div>
    {{/each}}
    `);
    const configuration = config;
    let pageModel = undefined;

    function moveElement(array, initialIndex, finalIndex) {
        array.splice(finalIndex, 0, array.splice(initialIndex, 1)[0])
        console.log(array);
        return array;
    }

    function movePositions(index, up) {
        pageModel = moveElement(pageModel, index, index + (up ? 1 : -1));
    }

    function rotate(index, clockwise) {
        pageModel[index].transformations.push(clockwise ? 'ROTATE_CLOCKWISE' : 'ROTATE_COUNTER_CLOCKWISE');
    }

    function markDeleted(index) {
        let transformations = pageModel[index].transformations;
        let isDeleted = transformations.includes('DELETE');
        if (isDeleted) {
            transformations.splice(transformations.indexOf('DELETE'), 1);
        } else {
            transformations.push('DELETE');
        }
    }

    function initActionListeners() {
        $('button', configuration.panel).on('click', function () {
            let button = $(this);
            let action = button.data('action');
            let index = button.data('index');
            console.log(`${action}@${index}`);

            switch (action) {
                case 'move-left':
                case 'move-right': {
                    movePositions(index, action === 'move-right');
                    break;
                }
                case 'rotate-clockwise':
                case 'rotate-counterclockwise': {
                    rotate(index, action === 'rotate-clockwise');
                    break;
                }
                case 'delete': {
                    markDeleted(index);
                    break
                }
            }
            render();
        })
    }

    function calculateAppearance(page) {
        let isDeleted = false;
        let rotationDirection = 0;
        for (const transformation of page.transformations) {
            switch (transformation) {
                case 'ROTATE_CLOCKWISE':
                    rotationDirection++;
                    break;
                case 'ROTATE_COUNTER_CLOCKWISE':
                    rotationDirection--;
                    break;
                case 'DELETE':
                    isDeleted = true;
                    break;
            }
        }

        let classes = isDeleted ? 'deleted ' : '';
        switch (rotationDirection % 4) {
            case 0:
                classes += '';
                break;
            case 1:
            case -3:
                classes += 'rotate-90';
                break;
            case 2:
            case -2:
                classes += 'rotate-180';
                break;
            case 3:
            case -1:
                classes += 'rotate-270';
                break;
        }
        return classes;
    }

    function render() {
        $(configuration.panel).html(null);
        const pageCount = pageModel.length + 1;
        let current = 1;
        for (const page of pageModel) {
            page.page.name = `${current}/${pageCount}`;
            page.page.style = calculateAppearance(page);
            current++;
        }
        $(configuration.panel).append(editPanelTemplate(pageModel));
        showPreview();
        initActionListeners();
    }

    this.init = function () {
        $.get(configuration.pages).done((data) => {
            pageModel = data;
            render();
        }).fail((error) => {
            console.log(error);
        })

        const saveButton = $(configuration.saveButton);
        saveButton.on('click', function () {
            $(saveButton).toggleClass('is-loading');
            $.ajax({
                url: configuration.updateUrl,
                type: 'POST',
                contentType: 'application/json; charset=utf-8',
                dataType: 'json',
                data: JSON.stringify(pageModel)
            })
                .done(() => window.location = configuration.appUrl)
                .fail((error) => console.error(error))
                .always(() => $(saveButton).toggleClass('is-loading'));
        });
    }

    function showPreview() {
        $(configuration.loadingContainer).hide();
        $(configuration.container).show();
    }
}
