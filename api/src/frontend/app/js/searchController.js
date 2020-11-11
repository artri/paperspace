
function SearchController(config) {
    const configuration = config;
    let timerId = -1;

    const searchIconTemplate = '<i class="fas fa-search"></i>'
    const clearSearchIconTemplate = '<i class="fas fa-times"></i>'

    function getLocationQueryParams() {
        return window.location.href.indexOf('?') !== -1 ? window.location.href.substr(window.location.href.indexOf('?')) : '';
    }

    function initializeSearchField() {
        window.onpopstate = function (event) {
            const query = getParameterByName('q', window.location);
            if (query) {
                $(configuration.input).val(query);
            } else {
                $(configuration.input).val('');
            }
            loadAvailableTags();
            search(configuration.endpoint + getLocationQueryParams());
            handleIcon();
        };
        if (!(window.history.state && window.history.state.url)) {
            search(configuration.endpoint + getLocationQueryParams());
        }
        handleIcon();
    }

    function handleIcon() {
        const searchInput = $(configuration.input);
        $('#searchButton').off('click');
        if (searchInput.val() === '') {
            $('#searchButton').html(searchIconTemplate);
            $('#searchButton').one('click', function () {
                handleSearchInput();
            })
        } else {
            $('#searchButton').html(clearSearchIconTemplate);
            $('#searchButton').one('click', function () {
                searchInput.val(null);
                handleSearchInput();
            })
        }
    }

    function getActiveTags() {
        const ids = [];
        const activeTags = $('#tags .active');
        for (const tag of activeTags) {
            ids.push($(tag).data('id'));
        }
        return ids;
    }

    function handleSearchInput() {
        let query = $(configuration.input).val();
        let tags = getActiveTags().join();
        let url = (query !== '' ? '?q=' + encodeURIComponent(query) : '');
        if (tags.length > 0) {
            url = url + (query !== '' ? '&tags=' + encodeURIComponent(tags) : '?tags=' + encodeURIComponent(tags));
        }
        window.history.pushState({
            query: query,
            tags: tags

        }, "", '/' + url);
        search(configuration.endpoint + url);
        handleIcon();
    }

    function loadAvailableTags() {
        $.get(configuration.tagEndpoint).done((data) => {
            $(configuration.tagContainer).html('');
            for (const tag of data) {
                $(configuration.tagContainer).append(`
                <button class="item button is-full-width is-text has-text-left is-block is-white" data-id="${tag.id}">
                    <span class="icon"><i class="fas fa-tag"></i></span>
                    <span class="name"><span>${tag.name}</span> <span></span></span>
                    <span class="icon is-pulled-right tag-remove"><i class="fas fa-minus-circle"></i></span>
                </button>`);
            }

            $('#tags button').on('click', function () {
                const tagId = $(this).data('id');
                if (tagId) {
                    $(this).toggleClass('active');
                } else {
                    $('#tags button').removeClass('active');
                }
                handleSearchInput();
            })
        }).fail((data) => {
            console.log(data);
            showError();
        })




    }

    this.init = function () {
        $(configuration.input).on('input', function (event) {
            handleIcon();
            if (timerId !== -1) {
                window.clearTimeout(timerId);
            }
            timerId = window.setTimeout(function () {
                handleSearchInput();
            }, configuration.timeout);
        })

        loadAvailableTags();
        initializeSearchField();
    }

    this.changed = function(type) {
        if (type === 'files') {
            let searchUrl;
            if (window.location.href.indexOf('?') !== - 1){
                searchUrl = configuration.endpoint +  getLocationQueryParams();
            } else {
                searchUrl = configuration.endpoint;
            }
            search(searchUrl);
        }

    }

    function renderResults(elements) {
        let result = '';
        elements.forEach(element => {
            let createdAt = DateTime.fromISO(element.createdAt);
            result += `
            <div id="${element.id}" class="column is-one-third is-one-quarter-fullhd">
            <a class="card bg-pan-bottom" href="${element.links.self}">
                <div class="card-content">
                    <div class="msg-header">
                        <div class=" msg-header has-text-right is-size-7">
                            <span>${createdAt.toLocaleString(DateTime.DATETIME_MED_WITH_SECONDS)}</span>
                        </div>
                    </div>
                    <div class="columns msg-snippet has-text-grey is-size-7 is-multiline">
                        <div class="column is-12">
                            <span class="document-icon">
                                <i class="${(element.type === 'TASK' ? (element.done ? 'fas fa-clipboard-check has-text-success' : 'fas fa-clipboard-check has-text-danger') : 'far fa-file')}"></i>
                            </span>
                            <figure class="image is-3by4">
                                <img src="${element.links.preview}">
                            </figure>
                        </div>
                        <div class="column is-12">
                            <div class="message-subject is-size-5 mb-2 is-bold text-black-50">${element.title}</div>
                            <div class="message-snippet">`+element.previewText+`</div>
                        </div>
                    </div>
                </div>
            </a></div>`
        });

        return result;
    }

    function renderPagination(data) {
        let paginationContainer = $(configuration.pagination);

        const paginationTemplate = `
            <div id="pagination" class="control">
                <span class="title">${data.pagination.startIndex}-${data.pagination.endIndex} of ${data.pagination.results}</span>
                <button class="button is-link" ${data.pagination.previous ? '' : 'disabled'} data-href="${data.pagination.previous}"><i class="fa fa-chevron-left"></i></button>
                <button class="button is-link" ${data.pagination.next ? '' : 'disabled'} data-href="${data.pagination.next}"><i class="fa fa-chevron-right"></i></button>
            </div>`
        paginationContainer.html(paginationTemplate);

        $('button', paginationContainer).on('click', function (event) {
            const url = $(this).data('href');
            search(url);

            window.history.pushState({
                query: $(configuration.input).val(),
                tags: getActiveTags().join()
            }, "", '/' + url.substr(url.indexOf('?')));
        });

    }

    function getParameterByName(name) {
        const url = window.location.href;
        name = name.replace(/[\[\]]/g, '\\$&');
        const regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
            results = regex.exec(url);
        if (!results) return null;
        if (!results[2]) return '';
        return decodeURIComponent(results[2].replace(/\+/g, ' '));
    }

    function updateTags(tagValues) {
        const tagContainer = $('#tags');
        for (const tagValue of tagValues) {
            const tagValueContainer = $(`[data-id=${tagValue.id}]`, tagContainer);
            if (tagValueContainer.length > 0) {
                $('.name span:nth-child(2)', tagValueContainer).text(`(${tagValue.count})`);
            }
            if (tagValue.active) {
                tagValueContainer.addClass('active')
            } else {
                tagValueContainer.removeClass('active')
            }
        }
    }

    function search(url) {
        showLoader();
        $.get(url).done((data) => {
            $(configuration.results).html('');
            if (data.items.length > 0) {
                $(configuration.results).append(renderResults(data.items));
                renderPagination(data);
            } else {
                $(configuration.results).html('<div class="column has-text-centered">no search results</div>');
            }
            updateTags(data.tags);
            showResults();
        }).fail((data) => {
            console.log(data);
            showError();
        })

    }

    function showLoader() {
        $(configuration.results).hide();
        $(configuration.pagination).hide();
        $(configuration.loading).show();
        $(configuration.error).hide();
    }

    function showError() {
        $(configuration.pagination).hide();
        $(configuration.results).hide();
        $(configuration.loading).hide();
        $(configuration.error).show();
    }

    function showResults() {
        $(configuration.pagination).fadeIn();
        $(configuration.results).fadeIn();
        $(configuration.loading).hide();
        $(configuration.error).hide();
    }
}
