function PreviewController(config) {
    const imageTemplate = Handlebars.compile(`
    <div class="mySlides fade">
        <img src="{{page.links.preview}}" style="width:100%">
    </div>
    `)

    const navigationTemplate = Handlebars.compile(`
<div style="text-align:center">
  {{#each .}}
  <span class="dot" data-index="{{@index}}"></span>
  {{/each}}
</div>
    `)

    const configuration = config;
    let slideIndex = 1;

    function isFallbackFullScreenModeNeeded() {
        if (document.cancelFullScreen) {
            return false;
        } else if (document.mozCancelFullScreen) {
            return false;
        } else if (document.webkitCancelFullScreen) {
            return false;
        } else return !document.msCancelFullScreen;
    }

    const useFallbackFullscreenMode = isFallbackFullScreenModeNeeded();

    function attachListeners() {
        $('.dot', configuration.gallery).on('click', function () {
            let slideToShow = $(this).data('index');
            currentSlide(slideIndex = (slideToShow + 1));
        })
        var swipe = swiper(document.querySelector(configuration.gallery), function (e) {
            if (e.direction === swipe.directions.left) {
                plusSlides(1);
            }
            if (e.direction === swipe.directions.right) {
                plusSlides(-1);
            }
        });
    }

    this.init = function () {
        if (useFallbackFullscreenMode) {
            console.warn("Using fallback fullscreen mode since there is no api for fullscreen.");
            $('#preview-container').addClass('fake-fullscreen');
        }
        $.get(configuration.pages).done((data) => {
            $(configuration.gallery).html(null);
            for (const item of data) {
                $(configuration.gallery).append(imageTemplate(item));
            }
            $(configuration.gallery).append(navigationTemplate(data));
            attachListeners();
            showPreview();
            currentSlide(1);
        }).fail((error) => {
            console.log(error);
        })
        let isFullscreen = false;

        $('.toggle').click(function (e) {
            e.preventDefault();
            $(this).toggleClass('toggle-on');
        });


        $('[data-controls=fullscreen]').on('click', function () {
            if (!isFullscreen) {
                if (!useFallbackFullscreenMode) {
                    fullScreen($(configuration.container)[0])
                }
                isFullscreen = true;
                $('#preview-container').addClass('fullscreen');
            } else {
                if (!useFallbackFullscreenMode) {
                    cancelFullScreen()
                }
                isFullscreen = false;
                $('#preview-container').removeClass('fullscreen');
            }
        })
        document.addEventListener("fullscreenchange", function (event) {
            isFullscreen = !!document.fullscreenElement;
            if (!isFullscreen) {
                $('#preview-container').removeClass('fullscreen');
            }
        });

        document.addEventListener('keydown', function (event) {
            const targetTagName = event.target.tagName;
            if (targetTagName !== 'TEXTAREA' && targetTagName !== 'INPUT' && !event.altKey && !event.ctrlKey) {
                switch (event.key) {
                    case 'ArrowLeft':
                        plusSlides(-1);
                        break;
                    case 'ArrowRight':
                        plusSlides(1);
                        break;
                }
            }
        });

    }

    function plusSlides(n) {
        showSlides(slideIndex += n);
    }

    function currentSlide(n) {
        showSlides(slideIndex = n);
        let previousButton = $('button.prev', configuration.controls);
        let nextButton = $('button.next', configuration.controls);

        previousButton.prop('disabled', n === 1);
        nextButton.prop('disabled', n === document.getElementsByClassName("mySlides").length);

        previousButton.off('click');
        nextButton.off('click');

        previousButton.on('click', function () {
            currentSlide(--slideIndex);
        })
        nextButton.on('click', function () {
            currentSlide(++slideIndex);
        })
    }

    function showSlides(n) {
        var i;
        var slides = document.getElementsByClassName("mySlides");
        var dots = document.getElementsByClassName("dot");
        if (n > slides.length) {
            slideIndex = 1
        } else if (n < 1) {
            slideIndex = slides.length
        } else {
            slideIndex = n;
        }
        for (i = 0; i < slides.length; i++) {
            slides[i].style.display = "none";
        }
        for (i = 0; i < dots.length; i++) {
            dots[i].className = dots[i].className.replace(" active", "");
        }
        slides[slideIndex - 1].style.display = "block";
        dots[slideIndex - 1].className += " active";
    }

    function cancelFullScreen() {
        if (document.cancelFullScreen) {
            document.cancelFullScreen();
        } else if (document.mozCancelFullScreen) {
            document.mozCancelFullScreen();
        } else if (document.webkitCancelFullScreen) {
            document.webkitCancelFullScreen();
        } else if (document.msCancelFullScreen) {
            document.msCancelFullScreen();
        } else {

        }
    }

    function fullScreen(element) {
        if (element.requestFullScreen) {
            element.requestFullScreen();
        } else if (element.webkitRequestFullScreen) {
            element.webkitRequestFullScreen();
        } else if (element.mozRequestFullScreen) {
            element.mozRequestFullScreen();
        } else {
            alert('bla')

        }
    }

    function showPreview() {
        $(configuration.loadingContainer).hide();
        $(configuration.container).show();
    }
}
