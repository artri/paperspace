const {series, parallel, src, dest, watch} = require('gulp');
const del = require('del');
const sass = require('gulp-sass');
const browserSync = require('browser-sync').create();
const concat = require('gulp-concat');
const { createProxyMiddleware } = require('http-proxy-middleware');


function clean(cb) {
    return del(['dist/']);
}

function cleanDist(cb) {
    return del(['../../src/main/resources/static/', '../../src/main/resources/templates/'], {force: true});
}

function css(cb) {
    return src('./app/sass/**/*.scss')
        .pipe(sass().on('error', sass.logError))
        .pipe(dest('./dist/css'))
        .pipe(browserSync.stream());
}

function font(cb) {
    return src(['./app/font/**', './node_modules/@fortawesome/fontawesome-free/webfonts/**'])
        .pipe(dest('./dist/font'))
        .pipe(browserSync.stream());
}

function serve(cb) {
    browserSync.init({
        server: "./app",
        serveStatic: ['./app', './dist/', './mock/'],
    });

    watch("app/sass/**/*.scss", series(css));
    watch("app/js/**/*.js", series(js));
    watch("app/font/**", series(font));
    watch("app/*.html").on('change', browserSync.reload);
    cb();
}

function serveDev(cb) {
    const proxy = createProxyMiddleware('/api', {target: 'http://localhost:8080'});

    browserSync.init({
        server: "./app",
        serveStatic: ['./app', './dist/'],
        middleware: [proxy]
    });

    watch("app/sass/**/*.scss", series(css));
    watch("app/js/**/*.js", series(js));
    watch("app/font/**", series(font));
    watch("app/*.html").on('change', browserSync.reload);
    cb();
}

function copyToTarget(cb) {
    src('./dist/**')
        .pipe(dest('../../src/main/resources/static/'));

    src('./app/*.html')
        .pipe(dest('../../src/main/resources/templates/'));
    cb();
}

function js(cb) {
    src('./app/js/*.js')
        .pipe(concat('main.js'))
        .pipe(dest('./dist/js'))
        .pipe(browserSync.stream());

    src('./app/js/vendor/***.js')
        .pipe(dest('./dist/js/vendor'))
        .pipe(browserSync.stream());
    src('./node_modules/bulma-calendar/dist/js/bulma-calendar.min.js')
        .pipe(dest('./dist/js/vendor'))
        .pipe(browserSync.stream());
    cb();
}

exports.serve = series(clean, parallel(css, js, font), serve);
exports.serveDev = series(clean, parallel(css, js, font), serveDev);
exports.build = series(clean, parallel(css, js, font));
exports.dist = series(clean, cleanDist, parallel(css, js, font), copyToTarget);
exports.default = serve;
