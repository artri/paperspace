function SearchController(config) {
    let configuration = config;
    let timerId = -1;

    function initializeSearchField() {
        if (window.history.state && window.history.state.url) {
            const query = getParameterByName('q', window.history.state.url);
            if (query) {
                $(configuration.input).val(query);
            } else {
                $(configuration.input).val('');
            }
            search(window.history.state.url, false);
        } else {
            $(configuration.input).val('');
            search('/search', false);
        }
        updateClearButtonState();
    }

    function updateClearButtonState() {
        if ($(configuration.input).val() === '') {
            $('#clear-search-button').hide();
        } else {
            $('#clear-search-button').show();
        }
    }

    this.init = function () {
        $(window).on('popstate', function () {
            console.log('pop history state ' + JSON.stringify(history.state))
            initializeSearchField();
        });
        $('#clear-search-button').on('click', function () {
            $(configuration.input).val(null);
            search('/search', true);
            updateClearButtonState();
        })
        $(configuration.input).on('input', function (event) {
            if (timerId !== -1) {
                window.clearTimeout(timerId);
            }
            timerId = window.setTimeout(function () {
                showLoader();
                let query = $(configuration.input).val();
                let url = '/search' + (query !== '' ? '?q=' + query : '');
                updateClearButtonState();
                search(url, true)
            }, configuration.timeout);
        })
        console.log(JSON.stringify(window.history.state));
        initializeSearchField();
    }

    function trimContent(content) {
        if (content) {
            return content;
        } else {
            return '';
        }
    }

    function renderIcon(element) {
        return element.type === 'DOCUMENT' ? `<svg class="bi bi-file-text mr-1" width="1em" height="1em" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
  <path fill-rule="evenodd" d="M4 1h8a2 2 0 012 2v10a2 2 0 01-2 2H4a2 2 0 01-2-2V3a2 2 0 012-2zm0 1a1 1 0 00-1 1v10a1 1 0 001 1h8a1 1 0 001-1V3a1 1 0 00-1-1H4z" clip-rule="evenodd"/>
  <path fill-rule="evenodd" d="M4.5 10.5A.5.5 0 015 10h3a.5.5 0 010 1H5a.5.5 0 01-.5-.5zm0-2A.5.5 0 015 8h6a.5.5 0 010 1H5a.5.5 0 01-.5-.5zm0-2A.5.5 0 015 6h6a.5.5 0 010 1H5a.5.5 0 01-.5-.5zm0-2A.5.5 0 015 4h6a.5.5 0 010 1H5a.5.5 0 01-.5-.5z" clip-rule="evenodd"/>
</svg>` : `<svg class="bi bi-file-earmark-check mb-1 ${element.done ? 'text-success' : 'text-danger'}" width="1em" height="1em" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
  <path d="M9 1H4a2 2 0 00-2 2v10a2 2 0 002 2h5v-1H4a1 1 0 01-1-1V3a1 1 0 011-1h5v2.5A1.5 1.5 0 0010.5 6H13v2h1V6L9 1z"/>
  <path fill-rule="evenodd" d="M15.854 10.146a.5.5 0 010 .708l-3 3a.5.5 0 01-.708 0l-1.5-1.5a.5.5 0 01.708-.708l1.146 1.147 2.646-2.647a.5.5 0 01.708 0z" clip-rule="evenodd"/>
</svg>`;
    }

    function createPreviewLink(element) {
        let previewLink;
        if (element && element.pages[0] && element.pages[0].preview) {
            previewLink = '/download/' + element.pages[0].preview.id;
        } else {
            previewLink = '';
        }
        return previewLink;
    }

    function renderFirst(element) {
        let previewLink = createPreviewLink(element);
        let icon = renderIcon(element);
        return `<div class="jumbotron text-white rounded bg-dark p-5 position-relative">
            <div class="row">
                <div class="col-md-6 px-0 text-muted">${element.createdAt}</div>
                <div class="col-md-8 px-0">
                    <h1 class="font-italic text-lowercase">
                        ${icon}           
                        ${element.title}
                    </h1>
                    <p class="lead text-muted">${trimContent(element.previewText)}</p>
                    <p class="lead mb-0"><a href="${element.links.self}" class="text-white font-weight-bold stretched-link">open ...</a></p>
                </div>
                <div class="col-md-4 d-flex justify-content-end">
                    <img src="${previewLink}" alt="" class="img-fluid">
                </div>
            </div>
        </div>`
    }

    function renderRemaining(elements) {
        let result = '';
        elements.forEach(element => {
            let previewLink = createPreviewLink(element);
            let icon = renderIcon(element);
            result += `
            <div class="col-md-6">
                <div class="row no-gutters border rounded overflow-hidden flex-md-row mb-4 shadow-sm h-md-250 position-relative">
                    <div class="col-md-7 p-4 d-flex flex-column position-static">
                        <h3 class="text-lowercase">
                            ${icon}
                            ${element.title}
                        </h3>
                        <div class="mb-1 ">${element.createdAt}</div>
                        <p class="card-text small text-muted">${trimContent(element.previewText)}</p>
                        <a href="${element.links.self}" class="stretched-link">open ...</a>
                    </div>
                    <div class="col-md-5 p-2">
                     <img src="${previewLink}" alt="" class="img-thumbnail preview-image">
                    </div>
                </div>
            </div>`
        });

        return result;
    }

    function renderPaginationLink(url, index, page) {
        if (index !== page) {
            return `<li class="page-item"><button class="page-link" data-href="${url}">${index + 1}</button></li>`;
        } else {
            return `<li class="page-item disabled"><span class="page-link">${index + 1}</span></li>`;
        }
    }

    function renderPagination(data) {
        let paginationContainer = $(configuration.resultsPagination);
        if (data.totalPages > 1) {
            paginationContainer.html(`<ul class="pagination pagination-sm"></ul>`);
            let paginationList = $('.pagination', paginationContainer);
            let paginationLinks = data.pagination;
            if (paginationLinks['previous']) {
                paginationList.append(`<li class="page-item"><button class="page-link" data-href="${paginationLinks['previous']}">Previous</button></li>`);
            } else {
                paginationList.append(`<li class="page-item disabled"><span class="page-link">Previous</span></li>`);
            }

            let pages = paginationLinks['pages'];
            if (pages.length < 6) {
                for (let i = 0; i < pages.length; i++) {
                    const paginationLinkElement = pages[i];
                    if (i !== data.page) {
                        paginationList.append(`<li class="page-item"><button class="page-link" data-href="${paginationLinkElement}">${i + 1}</button></li>`);
                    } else {
                        paginationList.append(`<li class="page-item disabled"><span class="page-link">${i + 1}</span></li>`);
                    }
                }
            } else if (data.page < 3) {
                paginationList.append(renderPaginationLink(pages[0], 0, data.page));
                paginationList.append(renderPaginationLink(pages[1], 1, data.page));
                paginationList.append(renderPaginationLink(pages[2], 2, data.page));
                paginationList.append(`<li class="page-item disabled"><span class="page-link">...</span></li>`);
                paginationList.append(renderPaginationLink(pages[data.totalPages - 1], data.totalPages - 1, data.page));
            } else if (data.page > data.totalPages - 3) {
                paginationList.append(renderPaginationLink(pages[0], 0, data.page));
                paginationList.append(`<li class="page-item disabled"><span class="page-link">...</span></li>`);
                paginationList.append(renderPaginationLink(pages[data.totalPages - 3], data.totalPages - 3, data.page));
                paginationList.append(renderPaginationLink(pages[data.totalPages - 2], data.totalPages - 2, data.page));
                paginationList.append(renderPaginationLink(pages[data.totalPages - 1], data.totalPages - 1, data.page));
            } else {
                paginationList.append(renderPaginationLink(pages[0], 0, data.page));
                paginationList.append(`<li class="page-item disabled"><span class="page-link">...</span></li>`);
                paginationList.append(renderPaginationLink(pages[data.page - 1], data.page - 1, data.page));
                paginationList.append(renderPaginationLink(pages[data.page], data.page, data.page));
                paginationList.append(renderPaginationLink(pages[data.page + 1], data.page + 1, data.page));

                paginationList.append(`<li class="page-item disabled"><span class="page-link">...</span></li>`);
                paginationList.append(renderPaginationLink(pages[data.totalPages - 1], data.totalPages - 1, data.page));
            }
            if (paginationLinks['next']) {
                paginationList.append(`<li class="page-item"><button class="page-link" data-href="${paginationLinks['next']}">Next</button></li>`);
            } else {
                paginationList.append(`<li class="page-item disabled"><span class="page-link">Next</span></li>`);
            }
        } else {
            paginationContainer.html('');
        }

        $('button', paginationContainer).on('click', function (event) {
            let searchUrl = event.currentTarget.getAttribute('data-href')
            search(searchUrl);
        });

    }

    function getParameterByName(name, url) {
        if (!url) url = window.location.href;
        name = name.replace(/[\[\]]/g, '\\$&');
        const regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
            results = regex.exec(url);
        if (!results) return null;
        if (!results[2]) return '';
        return decodeURIComponent(results[2].replace(/\+/g, ' '));
    }

    function search(url, safeHistory) {
        const query = getParameterByName('q', url);
        const page = getParameterByName('page', url);
        if (safeHistory) {
            console.log('push history state ' + JSON.stringify({url: url, query: query, page: page}))
            window.history.pushState({url: url, query: query, page: page}, null, '/');
        }

        $.get(url).done((data) => {
            $(configuration.resultsPanel).html('');
            if (data.results > 0) {
                if (data.items.length > 0) {
                    $(configuration.resultsPanel).append(renderFirst(data.items[0]));
                }
                let remainingContainer = $('<div class="row mb-2"></div>');
                $(configuration.resultsPanel).append(remainingContainer);
                remainingContainer.html(renderRemaining(data.items.splice(1)));
                renderPagination(data);
            } else {
                $(configuration.resultsPanel).html('<div class="text-center">no search results</div>');
            }
            showResults();
        }).fail((data) => {
            console.log(data);
            showError();
        })

    }

    function showLoader() {
        $(configuration.resultsPanel).hide();
        $(configuration.loadingPanel).show();
        $(configuration.errorPanel).hide();
    }

    function showError() {
        $(configuration.resultsPanel).hide();
        $(configuration.loadingPanel).hide();
        $(configuration.errorPanel).show();
    }

    function showResults() {
        $(configuration.resultsPanel).fadeIn();
        $(configuration.loadingPanel).hide();
        $(configuration.errorPanel).hide();
    }
}

function DocumentController(config) {
    let configuration = config;

    this.init = function () {
        var saveLadda = Ladda.create(document.querySelector(configuration.saveButton));
        let doneButton = document.querySelector(configuration.doneButton);
        if (doneButton) {
            var doneLadda = Ladda.create(doneButton);
            $(configuration.doneButton).on('click', (e) => {
                doneLadda.start();
                $.post(configuration.doneUrl)
                    .done(() => location.reload())
                    .fail((error) => {
                        console.error(error);
                    }).always(() => {
                    doneLadda.stop();
                });
            })
        }

        let deleteButton = document.querySelector(configuration.deleteButton);
        if (deleteButton) {
            var deleteLadda = Ladda.create(deleteButton);
            $(configuration.deleteButton).on('click', (e) => {
                deleteLadda.start();
                $.ajax({
                    url: configuration.deleteUrl,
                    type: 'DELETE'
                }).done(() => window.location = configuration.appUrl)
                    .fail((error) => {
                        console.error(error);
                    }).always(() => {
                    deleteLadda.stop();
                });
            })
        }
        $(configuration.saveButton).click((e) => {
            saveLadda.start();
            e.preventDefault();
            $.post(configuration.selfUrl, $(configuration.form).serialize())
                .done(() => location.reload())
                .fail((error) => {
                    console.error(error);
                }).always(() => {
                saveLadda.stop();
            });
            return false;
        })
    }
}