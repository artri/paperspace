function UploadController(config) {
    const configuration = config;

    this.init = function () {
        const fileInput = $(configuration.input);
        fileInput.on('change', function () {
            $(configuration.typeChooser).toggleClass('is-active');
            if (document.querySelector(configuration.input).files.length > 0) {
                $(configuration.filenameLabel).text(document.querySelector(configuration.input).files[0].name);
            }
        });

        $('.modal-card-body button', configuration.modal).on('click', function () {
            uploadFile($(this).data('type'))
        })
    }

    function uploadFile(type) {
        console.log(`Uploading file as ${type}`)

        var xhr = new XMLHttpRequest();
        xhr.upload.onprogress = function(e) {
            var percent = (e.position / e.totalSize);
            console.log(percent);
        };
        xhr.onreadystatechange = function(e) {
            if(this.readyState === 4) {
                $(configuration.typeChooser).toggleClass('is-active');
            }
        };

        const file = document.querySelector(configuration.input).files[0];

        var formData = new FormData();

        formData.append("fileName", file.name);
        formData.append("type", type);
        formData.append("file", file);
        formData.append("mimeType", file.type);

        xhr.open('POST', '/api/binary', true);
        xhr.send(formData);
    }
}