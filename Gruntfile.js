module.exports = function(grunt) {

    // Project configuration.
    grunt.initConfig({
        browserify: {
            options:    {
                transform: [require('grunt-react').browserify],
                watch: true,
                keepAlive: true
            },
            app: {
                src: 'resources/js/app.js',
                dest: 'resources/public/js/app.js'
            }
        }
    });

    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-browserify');
    grunt.loadNpmTasks('grunt-watchify');

    // Default task(s).
    grunt.registerTask('default', []);

};
