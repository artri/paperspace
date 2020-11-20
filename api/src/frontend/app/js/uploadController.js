function UploadController(config) {
    const configuration = config;

    this.init = function () {
        const fileInput = $(configuration.input);
        fileInput.on('change', function () {
            $('.modal-card-body button', configuration.modal).attr('disabled', null);
            $(configuration.filenameLabel).html('');
            $(configuration.typeChooser).toggleClass('is-active');
            const fileInput = document.querySelector(configuration.input);
            if (fileInput.files.length > 0) {
                let index = 0;
                for (const file of fileInput.files) {
                    $(configuration.filenameLabel).append(`
                        <div id="filename-${index}" class="upload-file">
                            <i class="fas fa-file-upload"></i>
                            <div style="grid-column: 2 / span 2;" class="is-ellipsis">${file.name}</div>
                        </div>
                        <div id="progress-${index++}" style="width: 0" class="upload-progress is-upload"></div>`);
                }
            }
        });

        $('.modal-card-body button', configuration.modal).on('click', function () {
            const fileInput = document.querySelector(configuration.input);
            const files = [];
            let index = 0;
            if (fileInput.files.length > 0) {
                for (const file of fileInput.files) {
                    files.push({id: index++, file: file});
                }
            }
            $('.modal-card-body button', configuration.modal).attr('disabled', 'disabled');
            uploadFiles($(this).data('type'), files);
        })
        $('.modal-card-foot button',configuration.modal).on('click', function (){
            $(configuration.input).val(null)
        })

        $('.delete',configuration.modal).on('click', function (){
            $(configuration.input).val(null)
        })

    }

    function uploadFiles(type, files) {
        console.log(`Uploading files as ${type}`)
        let fileCount = files.length;
        const single = fileCount === 1;
        for (const file of files) {
            const fileId = file.id;
            const progressIndicator = $(`#progress-${fileId}`);
            let xhr = new XMLHttpRequest();
            xhr.upload.onprogress = function (e) {
                const percent = (e.loaded / e.total);
                progressIndicator.css('width', `${percent * 100}%`);
                const filenameLabel = $(`#filename-${fileId}`);
                filenameLabel.replaceWith(`
                        <div id="filename-${fileId}" class="upload-file">
                            <i class="fas fa-file-upload"></i>
                            <div class="is-ellipsis">${file.file.name}</div>
                            <div>${Math.trunc(percent * 100)}%</div>
                        </div>`)
            };
            xhr.onreadystatechange = function () {
                if (this.readyState === 4) {
                    const filenameLabel = $(`#filename-${fileId}`);
                    if (this.status === 201) {
                        filenameLabel.replaceWith(`
                            <div id="filename-${fileId}" class="upload-file has-text-success-dark">
                                <i class="fas fa-check-circle"></i>
                                <div class="is-ellipsis">${file.file.name}</div>
                                <div>done</div>
                            </div>
                        `)
                        fileCount--;
                    } else if (this.status === 303) {
                        filenameLabel.replaceWith(`
                            <div id="filename-${fileId}" class="upload-file has-text-danger">
                                <i class="fas fa-exclamation-triangle"></i>
                                <div class="is-ellipsis">${file.file.name}</div>
                                <div>already uploaded</div>
                            </div>
                        `)
                    }
                    if (fileCount === 0) {
                        $(configuration.filenameLabel).html('');
                        $(configuration.filenameLabel).append(`<div class="has-text-success-dark"><i class="fas fa-check-circle"></i>${files.length} ${!single ? 'files' : 'file'} uploaded.</div>`)
                        window.setTimeout(function () {
                            $(configuration.typeChooser).toggleClass('is-active');
                        }, 1000);
                    }
                }
            };

            const formData = new FormData();
            formData.append("fileName", file.file.name);
            formData.append("type", type);
            formData.append("file", file.file);
            formData.append("mimeType", file.file.type);

            xhr.open('POST', '/api/binary', true);
            xhr.send(formData);
        }
    }
}