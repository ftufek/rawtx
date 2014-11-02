/** @jsx React.DOM */

var $ = require('jquery');
var React = require('react');
var ReactRouter = require('react-router');
var Routes = ReactRouter.Routes;
var Route = ReactRouter.Route;
var DefaultRoute = ReactRouter.DefaultRoute;
var NotFoundRoute = ReactRouter.NotFoundRoute;
var Redirect = ReactRouter.Redirect;

var Navigation = require('./components/Navigation.js');
var Home = require('./components/Home.js');
var Explorer = require('./components/Explorer.js');
var Information = require('./components/Information.js');

var App = React.createClass({
    render: function(){
        return (
            <div>
                <Navigation />

                <this.props.activeRouteHandler />
            </div>

        );
    }
});

$(document).ready(function (){
    React.renderComponent((
        <Routes location="history">
            <Route name="app" path="/" handler={App}>
                <Route name="explore" path="/explore" handler={Explorer} />
                <Route name="information" path="/information" handler={Information} />
                <DefaultRoute handler={Home} />
            </Route>
        </Routes>
    ), document.body);
});