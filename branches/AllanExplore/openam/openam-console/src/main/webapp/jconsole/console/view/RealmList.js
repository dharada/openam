/**
 * Created with IntelliJ IDEA.
 * User: allan
 * Date: 8/11/12
 * Time: 11:33
 * To change this template use File | Settings | File Templates.
 */




Ext.define('AM.view.RealmList', {
    extend: 'Ext.grid.Panel',
    alias:   'widget.realmlist',

    title:   'Realms',

    items:[
        {
            xtype:'combobox',
            fieldLabel:'Choose a Realm:',
            store:'RealmStore',
            queryMode:'local',
            displayFied:'name',
            valueField:'name',
        }
    ],

    initComponent: function() {
        console.log('Initializing REALMS');
        this.callParent(arguments);
    },
    chooseRealm: function(scope) {
        console.log('Initializing REALMS');

    }
});
